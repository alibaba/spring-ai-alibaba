const modulesFiles: any = import.meta.glob("./**.ts", { eager: true });
const fileList = [];
for (const key of Object.keys(modulesFiles)) {
  fileList.push(modulesFiles[key].default);
}
export default fileList;
