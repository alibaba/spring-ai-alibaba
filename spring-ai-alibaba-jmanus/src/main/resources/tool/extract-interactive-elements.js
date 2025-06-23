// /*
//  * Copyright 2025 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// /**
//  * Some code is derived from buildDomTree.js in the browser-use project
//  * @link https://github.com/browser-use/browser-use/blob/main/browser_use/dom/buildDomTree.js
//  */
//   (index) => {

//   const TMP = []
//   const ID = {"count": index}
//   const COMPUTED_STYLES = new WeakMap();
//   const XPATH_CACHE = new WeakMap();
//   const INTERACTIVE_ELEMENT_CACHE = new WeakMap();
//   const CURRENT_TIMESTAMP = Date.now();
//   const INTERACTIVE_ELEMENT_TAG = new Set([
//     "a",          // Links
//     "button",     // Buttons
//     "input",      // All input types (text, checkbox, radio, etc.)
//     "select",     // Dropdown menus
//     "textarea",   // Text areas
//     "details",    // Expandable details
//     "summary",    // Summary element (clickable part of details)
//     "label",      // Form labels (often clickable)
//     "option",     // Select options
//     "optgroup",   // Option groups
//     "fieldset",   // Form fieldsets (can be interactive with legend)
//     "legend",     // Fieldset legends
//   ]);

//   extract(document.body)
//   return parseElement()

//   function parseElement() {
//     const RES = []
//     for (const element of TMP) {
//       // Filter sub elements can be clicked
//       let skip = false
//       for (const child of element.childNodes) {
//         if (INTERACTIVE_ELEMENT_CACHE.has(child) && INTERACTIVE_ELEMENT_CACHE.get(child)) {
//           skip = true
//           break
//         }
//       }
//       const tagName = element.tagName.toLowerCase()
//       // if tagName is in INTERACTIVE_ELEMENT_TAG need to be processed
//       if (skip && !INTERACTIVE_ELEMENT_TAG.has(tagName)) {
//         continue
//       }
//       const index = ID.count++
//       let jManusId
//       if (element.setAttribute) {
//         jManusId = CURRENT_TIMESTAMP + "-" + index;
//         element.setAttribute("jmanus-id", jManusId)
//       }
//       const text = element.innerText
//       const outerHtml = element.outerHTML
//       const xpath = getXPathTree(element)
//       RES.push({tagName, text, outerHtml, index, xpath, jManusId})
//     }
//     return RES
//   }

//   function extract(element) {
//     for (const child of element.childNodes) {
//       extract(child)
//     }

//     // Process children, with special handling for iframes and rich text editors
//     if (element.tagName) {
//       const tagName = element.tagName.toLowerCase();
//       // iframe ignore
//       if (tagName === "iframe") {
//         return;
//       } else {
//         // Handle shadow DOM
//         if (element.shadowRoot) {
//           for (const child of element.shadowRoot.childNodes) {
//             extract(child);
//           }
//         }
//       }
//     }

//     let isInteractive = isInteractiveElement(element);
//     if (!isInteractive) {
//       return;
//     }
//     if (!isElementVisible(element)) {
//       return;
//     }
//     INTERACTIVE_ELEMENT_CACHE.set(element, isInteractive);
//     TMP.push(element)
//   }

//   /**
//    * Returns an XPath tree string for an element.
//    */
//   function getXPathTree(element, stopAtBoundary = true) {
//     if (XPATH_CACHE.has(element)) {
//       return XPATH_CACHE.get(element);
//     }
//     const segments = [];
//     let currentElement = element;

//     while (currentElement && currentElement.nodeType === Node.ELEMENT_NODE) {
//       // Stop if we hit a shadow root or iframe
//       if (stopAtBoundary &&
//         (currentElement.parentNode instanceof ShadowRoot ||
//           currentElement.parentNode instanceof HTMLIFrameElement)
//       ) {
//         break;
//       }

//       const position = getElementPosition(currentElement);
//       const tagName = currentElement.nodeName.toLowerCase();
//       const xpathIndex = position > 0 ? `[${position}]` : "";
//       segments.unshift(`${tagName}${xpathIndex}`);

//       currentElement = currentElement.parentNode;
//     }

//     const result = segments.join("/");
//     XPATH_CACHE.set(element, result);
//     return result;
//   }

//   function getElementPosition(currentElement) {
//     if (!currentElement.parentElement) {
//       return 0; // No parent means no siblings
//     }

//     const tagName = currentElement.nodeName.toLowerCase();

//     const siblings = Array.from(currentElement.parentElement.children)
//       .filter((sib) => sib.nodeName.toLowerCase() === tagName);

//     if (siblings.length === 1) {
//       return 0; // Only element of its type
//     }

//     const index = siblings.indexOf(currentElement) + 1; // 1-based index
//     return index;
//   }

//   function getCachedComputedStyle(element) {
//     if (!element) return null;

//     if (COMPUTED_STYLES.has(element)) {
//       return COMPUTED_STYLES.get(element);
//     }
//     let style = window.getComputedStyle(element);
//     if (style) {
//       COMPUTED_STYLES.set(element, style);
//     }
//     return style;
//   }

//   function isElementVisible(element) {
//     const style = getCachedComputedStyle(element);
//     return (
//       element.offsetWidth > 0 &&
//       element.offsetHeight > 0 &&
//       style.visibility !== "hidden" &&
//       style.display !== "none"
//     );
//   }

//   function isInteractiveElement(element) {
//     if (!element || element.nodeType !== Node.ELEMENT_NODE) {
//       return false;
//     }

//     // Cache the tagName and style lookups
//     const tagName = element.tagName.toLowerCase();
//     const style = getCachedComputedStyle(element);

//     // Define interactive cursors
//     const interactiveCursors = new Set([
//       'pointer',    // Link/clickable elements
//       'move',       // Movable elements
//       'text',       // Text selection
//       'grab',       // Grabbable elements
//       'grabbing',   // Currently grabbing
//       'cell',       // Table cell selection
//       'copy',       // Copy operation
//       'alias',      // Alias creation
//       'all-scroll', // Scrollable content
//       'col-resize', // Column resize
//       'context-menu', // Context menu available
//       'crosshair',  // Precise selection
//       'e-resize',   // East resize
//       'ew-resize',  // East-west resize
//       'help',       // Help available
//       'n-resize',   // North resize
//       'ne-resize',  // Northeast resize
//       'nesw-resize', // Northeast-southwest resize
//       'ns-resize',  // North-south resize
//       'nw-resize',  // Northwest resize
//       'nwse-resize', // Northwest-southeast resize
//       'row-resize', // Row resize
//       's-resize',   // South resize
//       'se-resize',  // Southeast resize
//       'sw-resize',  // Southwest resize
//       'vertical-text', // Vertical text selection
//       'w-resize',   // West resize
//       'zoom-in',    // Zoom in
//       'zoom-out'    // Zoom out
//     ]);

//     // Define non-interactive cursors
//     const nonInteractiveCursors = new Set([
//       'not-allowed', // Action not allowed
//       'no-drop',     // Drop not allowed
//       'wait',        // Processing
//       'progress',    // In progress
//       'initial',     // Initial value
//       'inherit'      // Inherited value
//       //? Let's just include all potentially clickable elements that are not specifically blocked
//       // 'none',        // No cursor
//       // 'default',     // Default cursor
//       // 'auto',        // Browser default
//     ]);

//     function doesElementHaveInteractivePointer(element) {
//       if (element.tagName.toLowerCase() === "html") return false;

//       if (interactiveCursors.has(style.cursor)) return true;

//       return false;
//     }

//     let isInteractiveCursor = doesElementHaveInteractivePointer(element);

//     // Genius fix for almost all interactive elements
//     if (isInteractiveCursor) {
//       return true;
//     }

//     // Define explicit disable attributes and properties
//     const explicitDisableTags = new Set([
//       'disabled',           // Standard disabled attribute
//       // 'aria-disabled',      // ARIA disabled state
//       'readonly',          // Read-only state
//       // 'aria-readonly',     // ARIA read-only state
//       // 'aria-hidden',       // Hidden from accessibility
//       // 'hidden',            // Hidden attribute
//       // 'inert',             // Inert attribute
//       // 'aria-inert',        // ARIA inert state
//       // 'tabindex="-1"',     // Removed from tab order
//       // 'aria-hidden="true"' // Hidden from screen readers
//     ]);

//     // handle inputs, select, checkbox, radio, textarea, button and make sure they are not cursor style disabled/not-allowed
//     if (INTERACTIVE_ELEMENT_TAG.has(tagName)) {
//       // Check for non-interactive cursor
//       if (nonInteractiveCursors.has(style.cursor)) {
//         return false;
//       }

//       // Check for explicit disable attributes
//       for (const disableTag of explicitDisableTags) {
//         if (element.hasAttribute(disableTag) ||
//           element.getAttribute(disableTag) === 'true' ||
//           element.getAttribute(disableTag) === '') {
//           return false;
//         }
//       }

//       // Check for disabled property on form elements
//       if (element.disabled) {
//         return false;
//       }

//       // Check for readonly property on form elements
//       if (element.readOnly) {
//         return false;
//       }

//       // Check for inert property
//       if (element.inert) {
//         return false;
//       }

//       return true;
//     }

//     const role = element.getAttribute("role");
//     const ariaRole = element.getAttribute("aria-role");

//     // Check for contenteditable attribute
//     if (element.getAttribute("contenteditable") === "true" || element.isContentEditable) {
//       return true;
//     }

//     // Added enhancement to capture dropdown interactive elements
//     if (element.classList && (
//       element.classList.contains("button") ||
//       element.classList.contains('dropdown-toggle') ||
//       element.getAttribute('data-index') ||
//       element.getAttribute('data-toggle') === 'dropdown' ||
//       element.getAttribute('aria-haspopup') === 'true'
//     )) {
//       return true;
//     }

//     const interactiveRoles = new Set([
//       'button',           // Directly clickable element
//       // 'link',            // Clickable link
//       // 'menuitem',        // Clickable menu item
//       'menuitemradio',   // Radio-style menu item (selectable)
//       'menuitemcheckbox', // Checkbox-style menu item (toggleable)
//       'radio',           // Radio button (selectable)
//       'checkbox',        // Checkbox (toggleable)
//       'tab',             // Tab (clickable to switch content)
//       'switch',          // Toggle switch (clickable to change state)
//       'slider',          // Slider control (draggable)
//       'spinbutton',      // Number input with up/down controls
//       'combobox',        // Dropdown with text input
//       'searchbox',       // Search input field
//       'textbox',         // Text input field
//       // 'listbox',         // Selectable list
//       'option',          // Selectable option in a list
//       'scrollbar'        // Scrollable control
//     ]);

//     // Basic role/attribute checks
//     const hasInteractiveRole =
//       INTERACTIVE_ELEMENT_TAG.has(tagName) ||
//       interactiveRoles.has(role) ||
//       interactiveRoles.has(ariaRole);

//     if (hasInteractiveRole) return true;

//     // check whether element has event listeners by window.getEventListeners
//     try {
//       if (typeof getEventListeners === 'function') {
//         const listeners = getEventListeners(element);
//         const mouseEvents = ['click', 'mousedown', 'mouseup', 'dblclick'];
//         for (const eventType of mouseEvents) {
//           if (listeners[eventType] && listeners[eventType].length > 0) {
//             return true; // Found a mouse interaction listener
//           }
//         }
//       }

//       const getEventListenersForNode = window.getEventListenersForNode;
//       if (typeof getEventListenersForNode === 'function') {
//         const listeners = getEventListenersForNode(element);
//         const interactionEvents = ['click', 'mousedown', 'mouseup', 'keydown', 'keyup', 'submit', 'change', 'input', 'focus', 'blur'];
//         for (const eventType of interactionEvents) {
//           for (const listener of listeners) {
//             if (listener.type === eventType) {
//               return true; // Found a common interaction listener
//             }
//           }
//         }
//       }
//       // Fallback: Check common event attributes if getEventListeners is not available (getEventListeners doesn't work in page.evaluate context)
//       const commonMouseAttrs = ['onclick', 'onmousedown', 'onmouseup', 'ondblclick'];
//       for (const attr of commonMouseAttrs) {
//         if (element.hasAttribute(attr) || typeof element[attr] === 'function') {
//           return true;
//         }
//       }
//     } catch (e) {
//       // console.warn(`Could not check event listeners for ${element.tagName}:`, e);
//       // If checking listeners fails, rely on other checks
//     }

//     return false
//   }
// }
