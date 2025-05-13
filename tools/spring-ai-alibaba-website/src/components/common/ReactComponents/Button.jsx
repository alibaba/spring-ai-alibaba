import Jump from "./Jump";

const Button = (props) => {
  const {
    href,
    type = "normal",
    size = "medium",
    visibility = true,
    target = "_self",
    children,
    iconClass = "",
    onClick,
  } = props;

  return (
    <span class="button-div">
      <button
        {...props}
        class="button w-fit p-0 bg-transparent"
        onClick={onClick}
      >
        <a
          href={href}
          target={target}
          class={`button-${
            type || "normal"
          } flex items-center justify-center no-underline ${
            size === "small" ? "xp-small h-small" : ""
          } ${size === "medium" ? "xp-medium h-medium" : ""} ${
            size === "large" ? "xp-large h-large" : ""
          } ${props?.class || ""}`}
        >
          {children}
          {visibility && (
            <span class="icon jump-icon">
              <Jump class={iconClass} />
            </span>
          )}
        </a>
      </button>
    </span>
  );
};

export default Button;
