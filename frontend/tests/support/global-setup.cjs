const { spawn, execFileSync } = require("node:child_process");
const { randomUUID } = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
module.exports = async () => {
  const name = "govener-browser-" + randomUUID().slice(0, 8);
  const root = path.resolve(__dirname, "../../..");
  const jar = path.join(
    root,
    "backend/target/govener-backend-0.1.0-SNAPSHOT.jar",
  );
  if (!fs.existsSync(jar))
    throw new Error(
      "Build the backend first: .\\mvnw.cmd -pl backend -DskipTests package",
    );
  const docker = (args) =>
    execFileSync("docker", args, {
      encoding: "utf8",
      windowsHide: true,
      timeout: 120000,
    }).trim();
  process.env.GOVENER_BROWSER_TEST_CONTAINER = name;
  let backend;
  const cleanup = () => {
    if (backend?.pid) {
      try {
        if (process.platform === "win32")
          execFileSync("taskkill", ["/pid", String(backend.pid), "/t", "/f"], {
            windowsHide: true,
            stdio: "ignore",
          });
        else backend.kill("SIGTERM");
      } catch {}
    }
    try {
      docker(["stop", name]);
    } catch {}
  };
  try {
    docker([
      "run",
      "--rm",
      "-d",
      "--name",
      name,
      "-p",
      "127.0.0.1::27017",
      "mongo:8.0.12",
      "--replSet",
      "rs0",
      "--bind_ip_all",
    ]);
    const mapped = docker(["port", name, "27017/tcp"]).split(":").pop();
    for (let i = 0; i < 40; i++) {
      try {
        docker([
          "exec",
          name,
          "mongosh",
          "--quiet",
          "--eval",
          'rs.initiate({_id:"rs0",members:[{_id:0,host:"localhost:27017"}]})',
        ]);
        break;
      } catch {
        await new Promise((resolve) => setTimeout(resolve, 250));
      }
    }
    const log = fs.openSync(path.join(root, "backend/browser-test.log"), "w");
    const java = process.env.JAVA_HOME
      ? path.join(
          process.env.JAVA_HOME,
          "bin",
          process.platform === "win32" ? "java.exe" : "java",
        )
      : "java";
    backend = spawn(
      java,
      [
        "-jar",
        jar,
        "--spring.profiles.active=dev",
        "--server.port=18081",
        `--spring.data.mongodb.uri=mongodb://127.0.0.1:${mapped}/govener_browser?replicaSet=rs0&directConnection=true`,
        "--debug=false",
        "--trace=false",
        "--app.allowed-origins=http://127.0.0.1:4175",
      ],
      { cwd: root, windowsHide: true, stdio: ["ignore", log, log] },
    );
    let healthy = false;
    for (let i = 0; i < 120; i++) {
      if (backend.exitCode !== null)
        throw new Error("Test backend stopped. See backend/browser-test.log");
      try {
        const result = await fetch("http://127.0.0.1:18081/actuator/health");
        if (result.ok) {
          healthy = true;
          break;
        }
      } catch {}
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
    if (!healthy) throw new Error("Test backend did not become healthy");
    let seeded = false;
    for (let i = 0; i < 40; i++) {
      if (
        docker([
          "exec",
          name,
          "mongosh",
          "govener_browser",
          "--quiet",
          "--eval",
          "Boolean(db.memberships.findOne({_id:'resident-membership'}))",
        ]).includes("true")
      ) {
        seeded = true;
        break;
      }
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
    if (!seeded) throw new Error("Demo seed did not finish");
    // Additional site grants are fixtures inside this disposable database only.
    docker([
      "exec",
      name,
      "mongosh",
      "govener_browser",
      "--quiet",
      "--eval",
      `
      const manager=db.memberships.findOne({_id:'manager-membership'});
      const resident=db.memberships.findOne({_id:'resident-membership'});
      if(!manager||!resident) throw new Error('Seed is not ready');
      for(const [source,unit] of [[manager,null],[resident,'b06']]){
        const id=source.data.userId+'-bosphorus';
        const copy={...source,data:{...source.data}}; copy._id=id;copy.communityId='bosphorus-homes';copy.data.id=id;copy.data.communityId='bosphorus-homes';copy.data.personId=id+'-person';
        copy.data.units=unit?[{unitId:unit,startsOn:'2026-11-01',endsOn:null}]:[];
        db.memberships.insertOne(copy);
        db.people.insertOne({_id:id+'-person',communityId:'bosphorus-homes',version:0,data:{id:id+'-person',communityId:'bosphorus-homes',name:source.data.userId,version:0}});
      }
    `,
    ]);
    return cleanup;
  } catch (error) {
    cleanup();
    throw error;
  }
};
