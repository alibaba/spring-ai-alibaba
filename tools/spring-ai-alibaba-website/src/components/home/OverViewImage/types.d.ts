export interface PanelProps {
  width?: number;
  height?: number;
  class?: string;
}

//Product
export interface ProductProps {
  image: string;
  label: string;
  hoverable: boolean;
  hoverContent: any;
}
//Arrow
export interface ArrowProps {
  d: string;
}
