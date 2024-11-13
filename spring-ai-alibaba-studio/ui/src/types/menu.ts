export type SubMenuItem = {
  label: string;
  key: string;
  icon?: React.ReactNode;
  children?: SubMenuItem[];
};