const Fork = (props) => {
  return (
      <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          version="1.1"
          width="16"
          height="18"
          viewBox="0 0 1024 1024"
          class={props.class}
      >
          <g transform="scale(1,-1) translate(0, -1024)">
              <path
                  d="M384 160a32 32 0 0 1 32-32h192a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-64v128h192a64 64 0 0 1 64 64v64h64a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-192a32 32 0 0 1-32-32v-192a32 32 0 0 1 32-32h64V576h-448v64h64a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-192a32 32 0 0 1-32-32v-192a32 32 0 0 1 32-32h64V576a64 64 0 0 1 64-64h192V384h-64a32 32 0 0 1-32-32v-192zM448 320h128V192H448v128z m-256 384v128h128v-128H192z m512 0v128h128v-128h-128z"
                  fill="currentColor"
                  fill-opacity="1"
              ></path>
          </g>
      </svg>
  );
};

export default Fork;