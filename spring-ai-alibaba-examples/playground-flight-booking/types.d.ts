declare module '*.module.css' {
  declare const styles: Record<string, string>;
  export default styles;
}
declare module '*.module.sass' {
  declare const styles: Record<string, string>;
  export default styles;
}
declare module '*.module.scss' {
  declare const styles: Record<string, string>;
  export default styles;
}
declare module '*.module.less' {
  declare const classes: Record<string, string>;
  export default classes;
}
declare module '*.module.styl' {
  declare const classes: Record<string, string>;
  export default classes;
}

/* CSS FILES */
declare module '*.css';
declare module '*.sass';
declare module '*.scss';
declare module '*.less';
declare module '*.styl';

/* IMAGES */
declare module '*.svg' {
  const ref: string;
  export default ref;
}
declare module '*.bmp' {
  const ref: string;
  export default ref;
}
declare module '*.gif' {
  const ref: string;
  export default ref;
}
declare module '*.jpg' {
  const ref: string;
  export default ref;
}
declare module '*.jpeg' {
  const ref: string;
  export default ref;
}
declare module '*.png' {
  const ref: string;
  export default ref;
}
declare module '*.avif' {
  const ref: string;
  export default ref;
}
declare module '*.webp' {
  const ref: string;
  export default ref;
}
declare module '*.css?inline' {
  import type { CSSResultGroup } from 'lit';
  const content: CSSResultGroup;
  export default content;
}

declare module 'csstype' {
  interface Properties {
    [index: `--${string}`]: any;
  }
}