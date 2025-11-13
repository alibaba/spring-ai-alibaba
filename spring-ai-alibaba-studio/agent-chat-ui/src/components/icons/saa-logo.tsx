export function SAALogoSVG({
  className,
  width,
  height,
}: {
  width?: number;
  height?: number;
  className?: string;
}) {
  return (
      <svg
        width={width || "280"}
        height={height || "70"}
        viewBox="0 0 280 70"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={className}
      >
        <defs>
          <linearGradient id="mainGreen" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#6db33f" stopOpacity="1" />
            <stop offset="100%" stopColor="#5a9f34" stopOpacity="1" />
          </linearGradient>

          <linearGradient id="accentGreen" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#7fc24a" stopOpacity="1" />
            <stop offset="100%" stopColor="#6db33f" stopOpacity="1" />
          </linearGradient>
        </defs>

        <g id="spring-ai">
          <text
            x="10"
            y="45"
            fontFamily="'Brush Script MT', 'Lucida Handwriting', cursive"
            fontSize="36"
            fontWeight="normal"
            fill="url(#mainGreen)"
            fontStyle="italic"
            letterSpacing="0"
          >
            Spring
          </text>

          <text
            x="100"
            y="45"
            fontFamily="'Brush Script MT', 'Lucida Handwriting', cursive"
            fontSize="36"
            fontWeight="normal"
            fill="url(#accentGreen)"
            fontStyle="italic"
            letterSpacing="0"
          >
            AI
          </text>
        </g>

        <g id="alibaba">
          <text
            x="155"
            y="45"
            fontFamily="'Brush Script MT', 'Lucida Handwriting', cursive"
            fontSize="30"
            fontWeight="normal"
            fill="url(#accentGreen)"
            fontStyle="italic"
            letterSpacing="1"
          >
            Alibaba
          </text>
        </g>

        <circle cx="150" cy="30" r="2.5" fill="#7fc24a" opacity="0.6"/>
        <circle cx="158" cy="35" r="2" fill="#6db33f" opacity="0.5"/>
        <circle cx="154" cy="42" r="1.8" fill="#5a9f34" opacity="0.7"/>
      </svg>

  );
}
