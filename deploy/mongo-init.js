const application = db.getSiblingDB("govener");
application.createUser({
  user: "govener_app",
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [{ role: "readWrite", db: "govener" }],
});
