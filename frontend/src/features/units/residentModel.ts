export type DirectoryUnit = {
  id: string;
  label: string;
  block: string;
  blockLabel?: string;
};
export type DirectoryPerson = { id: string; name: string };
export type CurrentResidence = { id: string; unitId: string; personId: string };
export type ResidentDirectorySnapshot = {
  asOf: string;
  currentPersonId: string;
  ownUnitIds: string[];
  units: DirectoryUnit[];
  people: DirectoryPerson[];
  residences: CurrentResidence[];
};
