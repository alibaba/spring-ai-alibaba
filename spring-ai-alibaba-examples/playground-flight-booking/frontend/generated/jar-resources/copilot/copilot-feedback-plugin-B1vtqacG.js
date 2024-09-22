import { x as d, b as u, l as h, s as f, P as v, t as b } from "./copilot-C5kdwofL.js";
import { r as p } from "./state-DIGAkjaT.js";
import { m, e as g } from "./overlay-monkeypatch-BJzx8nYb.js";
import { B as y } from "./base-panel-Bo-7csvK.js";
import { i as k } from "./icons-BFatMQTq.js";
const x = "copilot-feedback-panel{display:flex;flex-direction:column;font:var(--font-xsmall);--vaadin-input-field-label-font-size: var(--font-size-1);padding:var(--space-200);gap:var(--space-200);justify-content:space-between}copilot-feedback-panel>p{margin:0}copilot-feedback-panel .dialog-footer{display:flex;gap:var(--space-100)}copilot-feedback-panel vaadin-select,copilot-feedback-panel vaadin-text-area,copilot-feedback-panel vaadin-text-field{padding-top:0;--lumo-text-field-size: 1.75rem;--vaadin-input-field-label-font-size: var(--font-size-2);--vaadin-input-field-background: none;--vaadin-input-field-border-color: transparent;--vaadin-input-field-border-width: 1px;--vaadin-input-field-border-color: var(--border-color-high-contrast);--vaadin-input-field-hover-highlight: var(--gray-100);--vaadin-input-field-hover-highlight-opacity: 1}copilot-feedback-panel vaadin-text-area>textarea{max-height:7em}copilot-feedback-panel vaadin-text-area>textarea{padding:var(--space-100) 0;font:var(--font-xsmall)}copilot-feedback-panel vaadin-text-area:hover::part(input-field){background-color:var(--gray-100)}copilot-feedback-panel vaadin-text-field>input{font:var(--font-xsmall)}copilot-feedback-panel vaadin-select::part(input-field){border-radius:var(--radius-1);flex:1;padding:0 var(--space-50)}vaadin-select-overlay[theme=feedback]::part(overlay){--color-high-contrast: var(--gray-500)}copilot-feedback-panel vaadin-select[focus-ring]::part(input-field){box-shadow:none;outline:2px solid var(--selection-color);outline-offset:-2px}copilot-feedback-panel vaadin-select-value-button{padding:0 var(--space-50)}copilot-feedback-panel vaadin-select-item{--_lumo-selected-item-height: 1.75rem;--_lumo-selected-item-padding: 0;font:var(--font-xsmall)}copilot-feedback-panel vaadin-select-item:hover{background:none}";
var w = Object.defineProperty, $ = Object.getOwnPropertyDescriptor, o = (e, a, l, n) => {
  for (var t = n > 1 ? void 0 : n ? $(a, l) : a, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (t = (n ? r(a, l, t) : r(t)) || t);
  return n && t && w(a, l, t), t;
};
const c = "https://github.com/vaadin/copilot/issues/new", A = "?template=feature_request.md&title=%5BFEATURE%5D", P = "A short, concise description of the bug and why you consider it a bug. Any details like exceptions and logs can be helpful as well.", T = "Please provide as many details as possible, this will help us deliver a fix as soon as possible.%0AThank you!%0A%0A%23%23%23 Description of the Bug%0A%0A{description}%0A%0A%23%23%23 Expected Behavior%0A%0AA description of what you would expect to happen. (Sometimes it is clear what the expected outcome is if something does not work, other times, it is not super clear.)%0A%0A%23%23%23 Minimal Reproducible Example%0A%0AWe would appreciate the minimum code with which we can reproduce the issue.%0A%0A%23%23%23 Versions%0A{versionsInfo}";
let i = class extends y {
  constructor() {
    super(), this.description = "", this.items = [
      {
        label: "Report a Bug",
        value: "bug",
        ghTitle: "[BUG]"
      },
      {
        label: "Ask a Question",
        value: "question",
        ghTitle: "[QUESTION]"
      },
      {
        label: "Share an Idea",
        value: "idea",
        ghTitle: "[FEATURE]"
      }
    ];
  }
  render() {
    return d`<style>
        ${x}</style
      >${this.renderContent()}${this.renderFooter()}`;
  }
  firstUpdated() {
    m(this);
  }
  renderContent() {
    return this.message === void 0 ? d`
          <p>
            Your insights are incredibly valuable to us. Whether you’ve encountered a hiccup, have questions, or ideas
            to make our platform better, we're all ears! If you wish, leave your email and we’ll get back to you. You
            can even share your code snippet with us for a clearer picture.
          </p>
          <vaadin-select
            label="What's on your mind?"
            theme="feedback"
            .items="${this.items}"
            .value="${this.items[0].value}"
            @value-changed=${(e) => {
      this.type = e.detail.value;
    }}>
          </vaadin-select>
          <vaadin-text-area
            .value="${this.description}"
            @keydown=${this.keyDown}
            @focus=${() => {
      this.descriptionField.invalid = !1, this.descriptionField.placeholder = "";
    }}
            @value-changed=${(e) => {
      this.description = e.detail.value;
    }}
            label="Tell Us More"
            helper-text="Describe what you're experiencing, wondering about, or envisioning. The more you share, the better we can understand and act on your feedback"></vaadin-text-area>
          <vaadin-text-field
            @keydown=${this.keyDown}
            @value-changed=${(e) => {
      this.email = e.detail.value;
    }}
            id="email"
            label="Your Email (Optional)"
            helper-text="Leave your email if you’d like us to follow up. Totally optional, but we’d love to keep the conversation going."></vaadin-text-field>
        ` : d`<p>${this.message}</p>`;
  }
  renderFooter() {
    return this.message === void 0 ? d`
          <div class="dialog-footer">
            <vaadin-button
              theme="tertiary"
              @click="${() => u.emit("system-info-with-callback", {
      callback: (e) => this.openGithub(e, this),
      notify: !1
    })}">
              <span style="display: flex" slot="prefix">${k.github}</span>
              Create GitHub issue
            </vaadin-button>
            <div style="flex-grow: 1"></div>
            <vaadin-button theme="tertiary" @click="${this.close}">Cancel</vaadin-button>
            <vaadin-button theme="primary" @click="${this.submit}">Submit</vaadin-button>
          </div>
        ` : d` <div class="footer">
          <vaadin-button @click="${this.close}">Close</vaadin-button>
        </div>`;
  }
  close() {
    h.updatePanel("copilot-feedback-panel", {
      floating: !1
    });
  }
  submit() {
    if (this.description.trim() === "") {
      this.descriptionField.invalid = !0, this.descriptionField.placeholder = "Please tell us more before sending", this.descriptionField.value = "";
      return;
    }
    const e = {
      description: this.description,
      email: this.email,
      type: this.type
    };
    f(`${v}feedback`, e), this.parentNode?.style.setProperty("--section-height", "130px"), this.message = "Thank you for sharing feedback.";
  }
  keyDown(e) {
    (e.key === "Backspace" || e.key === "Delete") && e.stopPropagation();
  }
  openGithub(e, a) {
    if (this.type === "idea") {
      window.open(`${c}${A}`);
      return;
    }
    const l = e.replace(/\n/g, "%0A"), n = `${a.items.find((r) => r.value === this.type)?.ghTitle}`, t = a.description !== "" ? a.description : P, s = T.replace("{description}", t).replace("{versionsInfo}", l);
    window.open(`${c}?title=${n}&body=${s}`, "_blank")?.focus();
  }
};
o([
  p()
], i.prototype, "description", 2);
o([
  p()
], i.prototype, "type", 2);
o([
  p()
], i.prototype, "email", 2);
o([
  p()
], i.prototype, "message", 2);
o([
  p()
], i.prototype, "items", 2);
o([
  g("vaadin-text-area")
], i.prototype, "descriptionField", 2);
i = o([
  b("copilot-feedback-panel")
], i);
const F = {
  header: "Help Us Improve!",
  expanded: !0,
  expandable: !1,
  panelOrder: 0,
  floating: !1,
  tag: "copilot-feedback-panel",
  width: 500,
  height: 500,
  floatingPosition: {
    top: 50,
    left: 50
  }
}, D = {
  init(e) {
    e.addPanel(F);
  }
};
window.Vaadin.copilot.plugins.push(D);
export {
  i as CopilotFeedbackPanel
};
