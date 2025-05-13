import React, { useState, useEffect } from "react";
import "./index.css";

const Tooltip = ({ children, message, position = "top" }) => {
  const [showTooltip, setShowTooltip] = useState(false);

  const handleMouseEnter = () => {
    setShowTooltip(true);
  };

  const handleMouseLeave = () => {
    setShowTooltip(false);
  };

  const getTooltipPositionClass = () => {
    switch (position) {
      case "top":
        return "tooltip-top";
      case "bottom":
        return "tooltip-bottom";
      case "left":
        return "tooltip-left";
      case "right":
        return "tooltip-right";
      default:
        return "tooltip-top";
    }
  };

  return (
    <div
      className="tooltip-container"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
      {showTooltip && (
        <div className={`tooltip-content ${getTooltipPositionClass()}`}>
          {message}
        </div>
      )}
    </div>
  );
};

export default Tooltip;
