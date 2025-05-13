
const Close = (props) => {
  const { onClick } = props;
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      version="1.1"
      width="20"
      height="20"
      viewBox="0 0 20 20"
      class='ml-2'
      onClick={onClick}
      >
      <defs>
        <clipPath id="master_svg0_2989_08792">
          <rect x="4" y="4" width="12" height="12" rx="0" />
        </clipPath>
      </defs>
      <g><g>
        <ellipse cx="10" cy="10" rx="10" ry="10" fill="#EDF1FC" fill-opacity="1" />
      </g>
        <g clip-path="url(#master_svg0_2989_08792)"><g>
          <path
            d="M8.2,12.499951171875L7.5,11.799951171875L9.3,9.999951171875L7.5,8.212451171875L8.2,7.512451171875L10,9.312451171875L11.7875,7.512451171875L12.4875,8.212451171875L10.6875,9.999951171875L12.4875,11.799951171875L11.7875,12.499951171875L10,10.699951171875L8.2,12.499951171875Z"
            fill="#3E5CF4"
            fill-opacity="1" />
        </g></g></g>
    </svg>
  );
};

export default Close;