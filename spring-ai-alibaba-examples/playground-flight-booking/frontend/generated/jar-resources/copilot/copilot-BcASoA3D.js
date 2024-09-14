class qi extends EventTarget {
  constructor() {
    super(...arguments), this.eventBuffer = [], this.handledTypes = [];
  }
  on(t, r) {
    const n = r;
    return this.addEventListener(t, n), this.handledTypes.push(t), this.flush(t), () => this.off(t, n);
  }
  once(t, r) {
    this.addEventListener(t, r, { once: !0 });
  }
  off(t, r) {
    this.removeEventListener(t, r);
    const n = this.handledTypes.indexOf(t, 0);
    n > -1 && this.handledTypes.splice(n, 1);
  }
  emit(t, r) {
    const n = new CustomEvent(t, { detail: r });
    this.handledTypes.includes(t) || this.eventBuffer.push(n), this.dispatchEvent(n);
  }
  emitUnsafe({ type: t, data: r }) {
    this.emit(t, r);
  }
  // Communication with server via eventbus
  send(t, r) {
    const n = new CustomEvent("copilot-send", { detail: { command: t, data: r } });
    this.dispatchEvent(n);
  }
  // Listeners for Copilot itself
  onSend(t) {
    this.on("copilot-send", t);
  }
  offSend(t) {
    this.off("copilot-send", t);
  }
  flush(t) {
    const r = [];
    this.eventBuffer.filter((n) => n.type === t).forEach((n) => {
      this.dispatchEvent(n), r.push(n);
    }), this.eventBuffer = this.eventBuffer.filter((n) => !r.includes(n));
  }
}
var Hi = {
  0: "Invalid value for configuration 'enforceActions', expected 'never', 'always' or 'observed'",
  1: function(t, r) {
    return "Cannot apply '" + t + "' to '" + r.toString() + "': Field not found.";
  },
  /*
  2(prop) {
      return `invalid decorator for '${prop.toString()}'`
  },
  3(prop) {
      return `Cannot decorate '${prop.toString()}': action can only be used on properties with a function value.`
  },
  4(prop) {
      return `Cannot decorate '${prop.toString()}': computed can only be used on getter properties.`
  },
  */
  5: "'keys()' can only be used on observable objects, arrays, sets and maps",
  6: "'values()' can only be used on observable objects, arrays, sets and maps",
  7: "'entries()' can only be used on observable objects, arrays and maps",
  8: "'set()' can only be used on observable objects, arrays and maps",
  9: "'remove()' can only be used on observable objects, arrays and maps",
  10: "'has()' can only be used on observable objects, arrays and maps",
  11: "'get()' can only be used on observable objects, arrays and maps",
  12: "Invalid annotation",
  13: "Dynamic observable objects cannot be frozen. If you're passing observables to 3rd party component/function that calls Object.freeze, pass copy instead: toJS(observable)",
  14: "Intercept handlers should return nothing or a change object",
  15: "Observable arrays cannot be frozen. If you're passing observables to 3rd party component/function that calls Object.freeze, pass copy instead: toJS(observable)",
  16: "Modification exception: the internal structure of an observable array was changed.",
  17: function(t, r) {
    return "[mobx.array] Index out of bounds, " + t + " is larger than " + r;
  },
  18: "mobx.map requires Map polyfill for the current browser. Check babel-polyfill or core-js/es6/map.js",
  19: function(t) {
    return "Cannot initialize from classes that inherit from Map: " + t.constructor.name;
  },
  20: function(t) {
    return "Cannot initialize map from " + t;
  },
  21: function(t) {
    return "Cannot convert to map from '" + t + "'";
  },
  22: "mobx.set requires Set polyfill for the current browser. Check babel-polyfill or core-js/es6/set.js",
  23: "It is not possible to get index atoms from arrays",
  24: function(t) {
    return "Cannot obtain administration from " + t;
  },
  25: function(t, r) {
    return "the entry '" + t + "' does not exist in the observable map '" + r + "'";
  },
  26: "please specify a property",
  27: function(t, r) {
    return "no observable property '" + t.toString() + "' found on the observable object '" + r + "'";
  },
  28: function(t) {
    return "Cannot obtain atom from " + t;
  },
  29: "Expecting some object",
  30: "invalid action stack. did you forget to finish an action?",
  31: "missing option for computed: get",
  32: function(t, r) {
    return "Cycle detected in computation " + t + ": " + r;
  },
  33: function(t) {
    return "The setter of computed value '" + t + "' is trying to update itself. Did you intend to update an _observable_ value, instead of the computed property?";
  },
  34: function(t) {
    return "[ComputedValue '" + t + "'] It is not possible to assign a new value to a computed value.";
  },
  35: "There are multiple, different versions of MobX active. Make sure MobX is loaded only once or use `configure({ isolateGlobalState: true })`",
  36: "isolateGlobalState should be called before MobX is running any reactions",
  37: function(t) {
    return "[mobx] `observableArray." + t + "()` mutates the array in-place, which is not allowed inside a derivation. Use `array.slice()." + t + "()` instead";
  },
  38: "'ownKeys()' can only be used on observable objects",
  39: "'defineProperty()' can only be used on observable objects"
}, Fi = process.env.NODE_ENV !== "production" ? Hi : {};
function v(e) {
  for (var t = arguments.length, r = new Array(t > 1 ? t - 1 : 0), n = 1; n < t; n++)
    r[n - 1] = arguments[n];
  if (process.env.NODE_ENV !== "production") {
    var i = typeof e == "string" ? e : Fi[e];
    throw typeof i == "function" && (i = i.apply(null, r)), new Error("[MobX] " + i);
  }
  throw new Error(typeof e == "number" ? "[MobX] minified error nr: " + e + (r.length ? " " + r.map(String).join(",") : "") + ". Find the full error at: https://github.com/mobxjs/mobx/blob/main/packages/mobx/src/errors.ts" : "[MobX] " + e);
}
var Gi = {};
function mn() {
  return typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : Gi;
}
var yn = Object.assign, St = Object.getOwnPropertyDescriptor, G = Object.defineProperty, It = Object.prototype, $t = [];
Object.freeze($t);
var _r = {};
Object.freeze(_r);
var Wi = typeof Proxy < "u", Ji = /* @__PURE__ */ Object.toString();
function En() {
  Wi || v(process.env.NODE_ENV !== "production" ? "`Proxy` objects are not available in the current environment. Please configure MobX to enable a fallback implementation.`" : "Proxy not available");
}
function Me(e) {
  process.env.NODE_ENV !== "production" && h.verifyProxies && v("MobX is currently configured to be able to run in ES5 mode, but in ES5 MobX won't be able to " + e);
}
function U() {
  return ++h.mobxGuid;
}
function gr(e) {
  var t = !1;
  return function() {
    if (!t)
      return t = !0, e.apply(this, arguments);
  };
}
var De = function() {
};
function O(e) {
  return typeof e == "function";
}
function ve(e) {
  var t = typeof e;
  switch (t) {
    case "string":
    case "symbol":
    case "number":
      return !0;
  }
  return !1;
}
function Mt(e) {
  return e !== null && typeof e == "object";
}
function x(e) {
  if (!Mt(e))
    return !1;
  var t = Object.getPrototypeOf(e);
  if (t == null)
    return !0;
  var r = Object.hasOwnProperty.call(t, "constructor") && t.constructor;
  return typeof r == "function" && r.toString() === Ji;
}
function On(e) {
  var t = e?.constructor;
  return t ? t.name === "GeneratorFunction" || t.displayName === "GeneratorFunction" : !1;
}
function Ut(e, t, r) {
  G(e, t, {
    enumerable: !1,
    writable: !0,
    configurable: !0,
    value: r
  });
}
function An(e, t, r) {
  G(e, t, {
    enumerable: !1,
    writable: !1,
    configurable: !0,
    value: r
  });
}
function Oe(e, t) {
  var r = "isMobX" + e;
  return t.prototype[r] = !0, function(n) {
    return Mt(n) && n[r] === !0;
  };
}
function Te(e) {
  return e instanceof Map;
}
function ot(e) {
  return e instanceof Set;
}
var wn = typeof Object.getOwnPropertySymbols < "u";
function Xi(e) {
  var t = Object.keys(e);
  if (!wn)
    return t;
  var r = Object.getOwnPropertySymbols(e);
  return r.length ? [].concat(t, r.filter(function(n) {
    return It.propertyIsEnumerable.call(e, n);
  })) : t;
}
var We = typeof Reflect < "u" && Reflect.ownKeys ? Reflect.ownKeys : wn ? function(e) {
  return Object.getOwnPropertyNames(e).concat(Object.getOwnPropertySymbols(e));
} : (
  /* istanbul ignore next */
  Object.getOwnPropertyNames
);
function ir(e) {
  return typeof e == "string" ? e : typeof e == "symbol" ? e.toString() : new String(e).toString();
}
function Nn(e) {
  return e === null ? null : typeof e == "object" ? "" + e : e;
}
function B(e, t) {
  return It.hasOwnProperty.call(e, t);
}
var Zi = Object.getOwnPropertyDescriptors || function(t) {
  var r = {};
  return We(t).forEach(function(n) {
    r[n] = St(t, n);
  }), r;
};
function Lr(e, t) {
  for (var r = 0; r < t.length; r++) {
    var n = t[r];
    n.enumerable = n.enumerable || !1, n.configurable = !0, "value" in n && (n.writable = !0), Object.defineProperty(e, eo(n.key), n);
  }
}
function br(e, t, r) {
  return t && Lr(e.prototype, t), r && Lr(e, r), Object.defineProperty(e, "prototype", {
    writable: !1
  }), e;
}
function ne() {
  return ne = Object.assign ? Object.assign.bind() : function(e) {
    for (var t = 1; t < arguments.length; t++) {
      var r = arguments[t];
      for (var n in r)
        Object.prototype.hasOwnProperty.call(r, n) && (e[n] = r[n]);
    }
    return e;
  }, ne.apply(this, arguments);
}
function Sn(e, t) {
  e.prototype = Object.create(t.prototype), e.prototype.constructor = e, or(e, t);
}
function or(e, t) {
  return or = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function(n, i) {
    return n.__proto__ = i, n;
  }, or(e, t);
}
function Et(e) {
  if (e === void 0)
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  return e;
}
function Yi(e, t) {
  if (e) {
    if (typeof e == "string")
      return Ir(e, t);
    var r = Object.prototype.toString.call(e).slice(8, -1);
    if (r === "Object" && e.constructor && (r = e.constructor.name), r === "Map" || r === "Set")
      return Array.from(e);
    if (r === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(r))
      return Ir(e, t);
  }
}
function Ir(e, t) {
  (t == null || t > e.length) && (t = e.length);
  for (var r = 0, n = new Array(t); r < t; r++)
    n[r] = e[r];
  return n;
}
function Pe(e, t) {
  var r = typeof Symbol < "u" && e[Symbol.iterator] || e["@@iterator"];
  if (r)
    return (r = r.call(e)).next.bind(r);
  if (Array.isArray(e) || (r = Yi(e)) || t && e && typeof e.length == "number") {
    r && (e = r);
    var n = 0;
    return function() {
      return n >= e.length ? {
        done: !0
      } : {
        done: !1,
        value: e[n++]
      };
    };
  }
  throw new TypeError(`Invalid attempt to iterate non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`);
}
function Qi(e, t) {
  if (typeof e != "object" || e === null)
    return e;
  var r = e[Symbol.toPrimitive];
  if (r !== void 0) {
    var n = r.call(e, t || "default");
    if (typeof n != "object")
      return n;
    throw new TypeError("@@toPrimitive must return a primitive value.");
  }
  return (t === "string" ? String : Number)(e);
}
function eo(e) {
  var t = Qi(e, "string");
  return typeof t == "symbol" ? t : String(t);
}
var J = /* @__PURE__ */ Symbol("mobx-stored-annotations");
function W(e) {
  function t(r, n) {
    if (at(n))
      return e.decorate_20223_(r, n);
    Re(r, n, e);
  }
  return Object.assign(t, e);
}
function Re(e, t, r) {
  if (B(e, J) || Ut(e, J, ne({}, e[J])), process.env.NODE_ENV !== "production" && xt(r) && !B(e[J], t)) {
    var n = e.constructor.name + ".prototype." + t.toString();
    v("'" + n + "' is decorated with 'override', but no such decorated member was found on prototype.");
  }
  to(e, r, t), xt(r) || (e[J][t] = r);
}
function to(e, t, r) {
  if (process.env.NODE_ENV !== "production" && !xt(t) && B(e[J], r)) {
    var n = e.constructor.name + ".prototype." + r.toString(), i = e[J][r].annotationType_, o = t.annotationType_;
    v("Cannot apply '@" + o + "' to '" + n + "':" + (`
The field is already decorated with '@` + i + "'.") + `
Re-decorating fields is not allowed.
Use '@override' decorator for methods overridden by subclass.`);
  }
}
function at(e) {
  return typeof e == "object" && typeof e.kind == "string";
}
function kt(e, t) {
  process.env.NODE_ENV !== "production" && !t.includes(e.kind) && v("The decorator applied to '" + String(e.name) + "' cannot be used on a " + e.kind + " element");
}
var _ = /* @__PURE__ */ Symbol("mobx administration"), st = /* @__PURE__ */ function() {
  function e(r) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "Atom@" + U() : "Atom"), this.name_ = void 0, this.isPendingUnobservation_ = !1, this.isBeingObserved_ = !1, this.observers_ = /* @__PURE__ */ new Set(), this.diffValue_ = 0, this.lastAccessedBy_ = 0, this.lowestObserverState_ = b.NOT_TRACKING_, this.onBOL = void 0, this.onBUOL = void 0, this.name_ = r;
  }
  var t = e.prototype;
  return t.onBO = function() {
    this.onBOL && this.onBOL.forEach(function(n) {
      return n();
    });
  }, t.onBUO = function() {
    this.onBUOL && this.onBUOL.forEach(function(n) {
      return n();
    });
  }, t.reportObserved = function() {
    return qn(this);
  }, t.reportChanged = function() {
    L(), Hn(this), I();
  }, t.toString = function() {
    return this.name_;
  }, e;
}(), mr = /* @__PURE__ */ Oe("Atom", st);
function $n(e, t, r) {
  t === void 0 && (t = De), r === void 0 && (r = De);
  var n = new st(e);
  return t !== De && fa(n, t), r !== De && Qn(n, r), n;
}
function ro(e, t) {
  return e === t;
}
function no(e, t) {
  return xr(e, t);
}
function io(e, t) {
  return xr(e, t, 1);
}
function oo(e, t) {
  return Object.is ? Object.is(e, t) : e === t ? e !== 0 || 1 / e === 1 / t : e !== e && t !== t;
}
var Ce = {
  identity: ro,
  structural: no,
  default: oo,
  shallow: io
};
function fe(e, t, r) {
  return Ye(e) ? e : Array.isArray(e) ? w.array(e, {
    name: r
  }) : x(e) ? w.object(e, void 0, {
    name: r
  }) : Te(e) ? w.map(e, {
    name: r
  }) : ot(e) ? w.set(e, {
    name: r
  }) : typeof e == "function" && !ct(e) && !Ze(e) ? On(e) ? Ve(e) : Xe(r, e) : e;
}
function ao(e, t, r) {
  if (e == null || be(e) || ht(e) || Q(e) || je(e))
    return e;
  if (Array.isArray(e))
    return w.array(e, {
      name: r,
      deep: !1
    });
  if (x(e))
    return w.object(e, void 0, {
      name: r,
      deep: !1
    });
  if (Te(e))
    return w.map(e, {
      name: r,
      deep: !1
    });
  if (ot(e))
    return w.set(e, {
      name: r,
      deep: !1
    });
  process.env.NODE_ENV !== "production" && v("The shallow modifier / decorator can only used in combination with arrays, objects, maps and sets");
}
function Bt(e) {
  return e;
}
function so(e, t) {
  return process.env.NODE_ENV !== "production" && Ye(e) && v("observable.struct should not be used with observable values"), xr(e, t) ? t : e;
}
var lo = "override";
function xt(e) {
  return e.annotationType_ === lo;
}
function lt(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: co,
    extend_: uo,
    decorate_20223_: ho
  };
}
function co(e, t, r, n) {
  var i;
  if ((i = this.options_) != null && i.bound)
    return this.extend_(e, t, r, !1) === null ? 0 : 1;
  if (n === e.target_)
    return this.extend_(e, t, r, !1) === null ? 0 : 2;
  if (ct(r.value))
    return 1;
  var o = xn(e, this, t, r, !1);
  return G(n, t, o), 2;
}
function uo(e, t, r, n) {
  var i = xn(e, this, t, r);
  return e.defineProperty_(t, i, n);
}
function ho(e, t) {
  process.env.NODE_ENV !== "production" && kt(t, ["method", "field"]);
  var r = t.kind, n = t.name, i = t.addInitializer, o = this, a = function(c) {
    var u, d, f, p;
    return pe((u = (d = o.options_) == null ? void 0 : d.name) != null ? u : n.toString(), c, (f = (p = o.options_) == null ? void 0 : p.autoAction) != null ? f : !1);
  };
  if (r == "field") {
    i(function() {
      Re(this, n, o);
    });
    return;
  }
  if (r == "method") {
    var l;
    return ct(e) || (e = a(e)), (l = this.options_) != null && l.bound && i(function() {
      var s = this, c = s[n].bind(s);
      c.isMobxAction = !0, s[n] = c;
    }), e;
  }
  v("Cannot apply '" + o.annotationType_ + "' to '" + String(n) + "' (kind: " + r + "):" + (`
'` + o.annotationType_ + "' can only be used on properties with a function value."));
}
function vo(e, t, r, n) {
  var i = t.annotationType_, o = n.value;
  process.env.NODE_ENV !== "production" && !O(o) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on properties with a function value."));
}
function xn(e, t, r, n, i) {
  var o, a, l, s, c, u, d;
  i === void 0 && (i = h.safeDescriptors), vo(e, t, r, n);
  var f = n.value;
  if ((o = t.options_) != null && o.bound) {
    var p;
    f = f.bind((p = e.proxy_) != null ? p : e.target_);
  }
  return {
    value: pe(
      (a = (l = t.options_) == null ? void 0 : l.name) != null ? a : r.toString(),
      f,
      (s = (c = t.options_) == null ? void 0 : c.autoAction) != null ? s : !1,
      // https://github.com/mobxjs/mobx/discussions/3140
      (u = t.options_) != null && u.bound ? (d = e.proxy_) != null ? d : e.target_ : void 0
    ),
    // Non-configurable for classes
    // prevents accidental field redefinition in subclass
    configurable: i ? e.isPlainObject_ : !0,
    // https://github.com/mobxjs/mobx/pull/2641#issuecomment-737292058
    enumerable: !1,
    // Non-obsevable, therefore non-writable
    // Also prevents rewriting in subclass constructor
    writable: !i
  };
}
function Dn(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: fo,
    extend_: po,
    decorate_20223_: _o
  };
}
function fo(e, t, r, n) {
  var i;
  if (n === e.target_)
    return this.extend_(e, t, r, !1) === null ? 0 : 2;
  if ((i = this.options_) != null && i.bound && (!B(e.target_, t) || !Ze(e.target_[t])) && this.extend_(e, t, r, !1) === null)
    return 0;
  if (Ze(r.value))
    return 1;
  var o = Pn(e, this, t, r, !1, !1);
  return G(n, t, o), 2;
}
function po(e, t, r, n) {
  var i, o = Pn(e, this, t, r, (i = this.options_) == null ? void 0 : i.bound);
  return e.defineProperty_(t, o, n);
}
function _o(e, t) {
  var r;
  process.env.NODE_ENV !== "production" && kt(t, ["method"]);
  var n = t.name, i = t.addInitializer;
  return Ze(e) || (e = Ve(e)), (r = this.options_) != null && r.bound && i(function() {
    var o = this, a = o[n].bind(o);
    a.isMobXFlow = !0, o[n] = a;
  }), e;
}
function go(e, t, r, n) {
  var i = t.annotationType_, o = n.value;
  process.env.NODE_ENV !== "production" && !O(o) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on properties with a generator function value."));
}
function Pn(e, t, r, n, i, o) {
  o === void 0 && (o = h.safeDescriptors), go(e, t, r, n);
  var a = n.value;
  if (Ze(a) || (a = Ve(a)), i) {
    var l;
    a = a.bind((l = e.proxy_) != null ? l : e.target_), a.isMobXFlow = !0;
  }
  return {
    value: a,
    // Non-configurable for classes
    // prevents accidental field redefinition in subclass
    configurable: o ? e.isPlainObject_ : !0,
    // https://github.com/mobxjs/mobx/pull/2641#issuecomment-737292058
    enumerable: !1,
    // Non-obsevable, therefore non-writable
    // Also prevents rewriting in subclass constructor
    writable: !o
  };
}
function yr(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: bo,
    extend_: mo,
    decorate_20223_: yo
  };
}
function bo(e, t, r) {
  return this.extend_(e, t, r, !1) === null ? 0 : 1;
}
function mo(e, t, r, n) {
  return Eo(e, this, t, r), e.defineComputedProperty_(t, ne({}, this.options_, {
    get: r.get,
    set: r.set
  }), n);
}
function yo(e, t) {
  process.env.NODE_ENV !== "production" && kt(t, ["getter"]);
  var r = this, n = t.name, i = t.addInitializer;
  return i(function() {
    var o = Le(this)[_], a = ne({}, r.options_, {
      get: e,
      context: this
    });
    a.name || (a.name = process.env.NODE_ENV !== "production" ? o.name_ + "." + n.toString() : "ObservableObject." + n.toString()), o.values_.set(n, new _e(a));
  }), function() {
    return this[_].getObservablePropValue_(n);
  };
}
function Eo(e, t, r, n) {
  var i = t.annotationType_, o = n.get;
  process.env.NODE_ENV !== "production" && !o && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on getter(+setter) properties."));
}
function zt(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: Oo,
    extend_: Ao,
    decorate_20223_: wo
  };
}
function Oo(e, t, r) {
  return this.extend_(e, t, r, !1) === null ? 0 : 1;
}
function Ao(e, t, r, n) {
  var i, o;
  return No(e, this, t, r), e.defineObservableProperty_(t, r.value, (i = (o = this.options_) == null ? void 0 : o.enhancer) != null ? i : fe, n);
}
function wo(e, t) {
  if (process.env.NODE_ENV !== "production") {
    if (t.kind === "field")
      throw v("Please use `@observable accessor " + String(t.name) + "` instead of `@observable " + String(t.name) + "`");
    kt(t, ["accessor"]);
  }
  var r = this, n = t.kind, i = t.name, o = /* @__PURE__ */ new WeakSet();
  function a(l, s) {
    var c, u, d = Le(l)[_], f = new de(s, (c = (u = r.options_) == null ? void 0 : u.enhancer) != null ? c : fe, process.env.NODE_ENV !== "production" ? d.name_ + "." + i.toString() : "ObservableObject." + i.toString(), !1);
    d.values_.set(i, f), o.add(l);
  }
  if (n == "accessor")
    return {
      get: function() {
        return o.has(this) || a(this, e.get.call(this)), this[_].getObservablePropValue_(i);
      },
      set: function(s) {
        return o.has(this) || a(this, s), this[_].setObservablePropValue_(i, s);
      },
      init: function(s) {
        return o.has(this) || a(this, s), s;
      }
    };
}
function No(e, t, r, n) {
  var i = t.annotationType_;
  process.env.NODE_ENV !== "production" && !("value" in n) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' cannot be used on getter/setter properties"));
}
var So = "true", $o = /* @__PURE__ */ Cn();
function Cn(e) {
  return {
    annotationType_: So,
    options_: e,
    make_: xo,
    extend_: Do,
    decorate_20223_: Po
  };
}
function xo(e, t, r, n) {
  var i, o;
  if (r.get)
    return Kt.make_(e, t, r, n);
  if (r.set) {
    var a = pe(t.toString(), r.set);
    return n === e.target_ ? e.defineProperty_(t, {
      configurable: h.safeDescriptors ? e.isPlainObject_ : !0,
      set: a
    }) === null ? 0 : 2 : (G(n, t, {
      configurable: !0,
      set: a
    }), 2);
  }
  if (n !== e.target_ && typeof r.value == "function") {
    var l;
    if (On(r.value)) {
      var s, c = (s = this.options_) != null && s.autoBind ? Ve.bound : Ve;
      return c.make_(e, t, r, n);
    }
    var u = (l = this.options_) != null && l.autoBind ? Xe.bound : Xe;
    return u.make_(e, t, r, n);
  }
  var d = ((i = this.options_) == null ? void 0 : i.deep) === !1 ? w.ref : w;
  if (typeof r.value == "function" && (o = this.options_) != null && o.autoBind) {
    var f;
    r.value = r.value.bind((f = e.proxy_) != null ? f : e.target_);
  }
  return d.make_(e, t, r, n);
}
function Do(e, t, r, n) {
  var i, o;
  if (r.get)
    return Kt.extend_(e, t, r, n);
  if (r.set)
    return e.defineProperty_(t, {
      configurable: h.safeDescriptors ? e.isPlainObject_ : !0,
      set: pe(t.toString(), r.set)
    }, n);
  if (typeof r.value == "function" && (i = this.options_) != null && i.autoBind) {
    var a;
    r.value = r.value.bind((a = e.proxy_) != null ? a : e.target_);
  }
  var l = ((o = this.options_) == null ? void 0 : o.deep) === !1 ? w.ref : w;
  return l.extend_(e, t, r, n);
}
function Po(e, t) {
  v("'" + this.annotationType_ + "' cannot be used as a decorator");
}
var Co = "observable", Vo = "observable.ref", To = "observable.shallow", Ro = "observable.struct", Vn = {
  deep: !0,
  name: void 0,
  defaultDecorator: void 0,
  proxy: !0
};
Object.freeze(Vn);
function ft(e) {
  return e || Vn;
}
var ar = /* @__PURE__ */ zt(Co), jo = /* @__PURE__ */ zt(Vo, {
  enhancer: Bt
}), Lo = /* @__PURE__ */ zt(To, {
  enhancer: ao
}), Io = /* @__PURE__ */ zt(Ro, {
  enhancer: so
}), Tn = /* @__PURE__ */ W(ar);
function pt(e) {
  return e.deep === !0 ? fe : e.deep === !1 ? Bt : Uo(e.defaultDecorator);
}
function Mo(e) {
  var t;
  return e ? (t = e.defaultDecorator) != null ? t : Cn(e) : void 0;
}
function Uo(e) {
  var t, r;
  return e && (t = (r = e.options_) == null ? void 0 : r.enhancer) != null ? t : fe;
}
function Rn(e, t, r) {
  if (at(t))
    return ar.decorate_20223_(e, t);
  if (ve(t)) {
    Re(e, t, ar);
    return;
  }
  return Ye(e) ? e : x(e) ? w.object(e, t, r) : Array.isArray(e) ? w.array(e, t) : Te(e) ? w.map(e, t) : ot(e) ? w.set(e, t) : typeof e == "object" && e !== null ? e : w.box(e, t);
}
yn(Rn, Tn);
var ko = {
  box: function(t, r) {
    var n = ft(r);
    return new de(t, pt(n), n.name, !0, n.equals);
  },
  array: function(t, r) {
    var n = ft(r);
    return (h.useProxies === !1 || n.proxy === !1 ? La : Sa)(t, pt(n), n.name);
  },
  map: function(t, r) {
    var n = ft(r);
    return new si(t, pt(n), n.name);
  },
  set: function(t, r) {
    var n = ft(r);
    return new ui(t, pt(n), n.name);
  },
  object: function(t, r, n) {
    return we(function() {
      return ti(h.useProxies === !1 || n?.proxy === !1 ? Le({}, n) : Aa({}, n), t, r);
    });
  },
  ref: /* @__PURE__ */ W(jo),
  shallow: /* @__PURE__ */ W(Lo),
  deep: Tn,
  struct: /* @__PURE__ */ W(Io)
}, w = /* @__PURE__ */ yn(Rn, ko), jn = "computed", Bo = "computed.struct", sr = /* @__PURE__ */ yr(jn), zo = /* @__PURE__ */ yr(Bo, {
  equals: Ce.structural
}), Kt = function(t, r) {
  if (at(r))
    return sr.decorate_20223_(t, r);
  if (ve(r))
    return Re(t, r, sr);
  if (x(t))
    return W(yr(jn, t));
  process.env.NODE_ENV !== "production" && (O(t) || v("First argument to `computed` should be an expression."), O(r) && v("A setter as second argument is no longer supported, use `{ set: fn }` option instead"));
  var n = x(r) ? r : {};
  return n.get = t, n.name || (n.name = t.name || ""), new _e(n);
};
Object.assign(Kt, sr);
Kt.struct = /* @__PURE__ */ W(zo);
var Mr, Ur, Dt = 0, Ko = 1, qo = (Mr = (Ur = /* @__PURE__ */ St(function() {
}, "name")) == null ? void 0 : Ur.configurable) != null ? Mr : !1, kr = {
  value: "action",
  configurable: !0,
  writable: !1,
  enumerable: !1
};
function pe(e, t, r, n) {
  r === void 0 && (r = !1), process.env.NODE_ENV !== "production" && (O(t) || v("`action` can only be invoked on functions"), (typeof e != "string" || !e) && v("actions should have valid names, got: '" + e + "'"));
  function i() {
    return Ln(e, r, t, n || this, arguments);
  }
  return i.isMobxAction = !0, i.toString = function() {
    return t.toString();
  }, qo && (kr.value = e, G(i, "name", kr)), i;
}
function Ln(e, t, r, n, i) {
  var o = Ho(e, t, n, i);
  try {
    return r.apply(n, i);
  } catch (a) {
    throw o.error_ = a, a;
  } finally {
    Fo(o);
  }
}
function Ho(e, t, r, n) {
  var i = process.env.NODE_ENV !== "production" && $() && !!e, o = 0;
  if (process.env.NODE_ENV !== "production" && i) {
    o = Date.now();
    var a = n ? Array.from(n) : $t;
    P({
      type: Or,
      name: e,
      object: r,
      arguments: a
    });
  }
  var l = h.trackingDerivation, s = !t || !l;
  L();
  var c = h.allowStateChanges;
  s && (Ae(), c = qt(!0));
  var u = Er(!0), d = {
    runAsAction_: s,
    prevDerivation_: l,
    prevAllowStateChanges_: c,
    prevAllowStateReads_: u,
    notifySpy_: i,
    startTime_: o,
    actionId_: Ko++,
    parentActionId_: Dt
  };
  return Dt = d.actionId_, d;
}
function Fo(e) {
  Dt !== e.actionId_ && v(30), Dt = e.parentActionId_, e.error_ !== void 0 && (h.suppressReactionErrors = !0), Ht(e.prevAllowStateChanges_), qe(e.prevAllowStateReads_), I(), e.runAsAction_ && Y(e.prevDerivation_), process.env.NODE_ENV !== "production" && e.notifySpy_ && C({
    time: Date.now() - e.startTime_
  }), h.suppressReactionErrors = !1;
}
function Go(e, t) {
  var r = qt(e);
  try {
    return t();
  } finally {
    Ht(r);
  }
}
function qt(e) {
  var t = h.allowStateChanges;
  return h.allowStateChanges = e, t;
}
function Ht(e) {
  h.allowStateChanges = e;
}
var In, Wo = "create";
In = Symbol.toPrimitive;
var de = /* @__PURE__ */ function(e) {
  Sn(t, e);
  function t(n, i, o, a, l) {
    var s;
    return o === void 0 && (o = process.env.NODE_ENV !== "production" ? "ObservableValue@" + U() : "ObservableValue"), a === void 0 && (a = !0), l === void 0 && (l = Ce.default), s = e.call(this, o) || this, s.enhancer = void 0, s.name_ = void 0, s.equals = void 0, s.hasUnreportedChange_ = !1, s.interceptors_ = void 0, s.changeListeners_ = void 0, s.value_ = void 0, s.dehancer = void 0, s.enhancer = i, s.name_ = o, s.equals = l, s.value_ = i(n, void 0, o), process.env.NODE_ENV !== "production" && a && $() && ge({
      type: Wo,
      object: Et(s),
      observableKind: "value",
      debugObjectName: s.name_,
      newValue: "" + s.value_
    }), s;
  }
  var r = t.prototype;
  return r.dehanceValue = function(i) {
    return this.dehancer !== void 0 ? this.dehancer(i) : i;
  }, r.set = function(i) {
    var o = this.value_;
    if (i = this.prepareNewValue_(i), i !== h.UNCHANGED) {
      var a = $();
      process.env.NODE_ENV !== "production" && a && P({
        type: k,
        object: this,
        observableKind: "value",
        debugObjectName: this.name_,
        newValue: i,
        oldValue: o
      }), this.setNewValue_(i), process.env.NODE_ENV !== "production" && a && C();
    }
  }, r.prepareNewValue_ = function(i) {
    if (F(this), R(this)) {
      var o = j(this, {
        object: this,
        type: k,
        newValue: i
      });
      if (!o)
        return h.UNCHANGED;
      i = o.newValue;
    }
    return i = this.enhancer(i, this.value_, this.name_), this.equals(this.value_, i) ? h.UNCHANGED : i;
  }, r.setNewValue_ = function(i) {
    var o = this.value_;
    this.value_ = i, this.reportChanged(), z(this) && K(this, {
      type: k,
      object: this,
      newValue: i,
      oldValue: o
    });
  }, r.get = function() {
    return this.reportObserved(), this.dehanceValue(this.value_);
  }, r.intercept_ = function(i) {
    return ut(this, i);
  }, r.observe_ = function(i, o) {
    return o && i({
      observableKind: "value",
      debugObjectName: this.name_,
      object: this,
      type: k,
      newValue: this.value_,
      oldValue: void 0
    }), dt(this, i);
  }, r.raw = function() {
    return this.value_;
  }, r.toJSON = function() {
    return this.get();
  }, r.toString = function() {
    return this.name_ + "[" + this.value_ + "]";
  }, r.valueOf = function() {
    return Nn(this.get());
  }, r[In] = function() {
    return this.valueOf();
  }, t;
}(st), Mn;
Mn = Symbol.toPrimitive;
var _e = /* @__PURE__ */ function() {
  function e(r) {
    this.dependenciesState_ = b.NOT_TRACKING_, this.observing_ = [], this.newObserving_ = null, this.isBeingObserved_ = !1, this.isPendingUnobservation_ = !1, this.observers_ = /* @__PURE__ */ new Set(), this.diffValue_ = 0, this.runId_ = 0, this.lastAccessedBy_ = 0, this.lowestObserverState_ = b.UP_TO_DATE_, this.unboundDepsCount_ = 0, this.value_ = new Pt(null), this.name_ = void 0, this.triggeredBy_ = void 0, this.isComputing_ = !1, this.isRunningSetter_ = !1, this.derivation = void 0, this.setter_ = void 0, this.isTracing_ = M.NONE, this.scope_ = void 0, this.equals_ = void 0, this.requiresReaction_ = void 0, this.keepAlive_ = void 0, this.onBOL = void 0, this.onBUOL = void 0, r.get || v(31), this.derivation = r.get, this.name_ = r.name || (process.env.NODE_ENV !== "production" ? "ComputedValue@" + U() : "ComputedValue"), r.set && (this.setter_ = pe(process.env.NODE_ENV !== "production" ? this.name_ + "-setter" : "ComputedValue-setter", r.set)), this.equals_ = r.equals || (r.compareStructural || r.struct ? Ce.structural : Ce.default), this.scope_ = r.context, this.requiresReaction_ = r.requiresReaction, this.keepAlive_ = !!r.keepAlive;
  }
  var t = e.prototype;
  return t.onBecomeStale_ = function() {
    ea(this);
  }, t.onBO = function() {
    this.onBOL && this.onBOL.forEach(function(n) {
      return n();
    });
  }, t.onBUO = function() {
    this.onBUOL && this.onBUOL.forEach(function(n) {
      return n();
    });
  }, t.get = function() {
    if (this.isComputing_ && v(32, this.name_, this.derivation), h.inBatch === 0 && // !globalState.trackingDerivatpion &&
    this.observers_.size === 0 && !this.keepAlive_)
      lr(this) && (this.warnAboutUntrackedRead_(), L(), this.value_ = this.computeValue_(!1), I());
    else if (qn(this), lr(this)) {
      var n = h.trackingContext;
      this.keepAlive_ && !n && (h.trackingContext = this), this.trackAndCompute() && Qo(this), h.trackingContext = n;
    }
    var i = this.value_;
    if (Ot(i))
      throw i.cause;
    return i;
  }, t.set = function(n) {
    if (this.setter_) {
      this.isRunningSetter_ && v(33, this.name_), this.isRunningSetter_ = !0;
      try {
        this.setter_.call(this.scope_, n);
      } finally {
        this.isRunningSetter_ = !1;
      }
    } else
      v(34, this.name_);
  }, t.trackAndCompute = function() {
    var n = this.value_, i = (
      /* see #1208 */
      this.dependenciesState_ === b.NOT_TRACKING_
    ), o = this.computeValue_(!0), a = i || Ot(n) || Ot(o) || !this.equals_(n, o);
    return a && (this.value_ = o, process.env.NODE_ENV !== "production" && $() && ge({
      observableKind: "computed",
      debugObjectName: this.name_,
      object: this.scope_,
      type: "update",
      oldValue: n,
      newValue: o
    })), a;
  }, t.computeValue_ = function(n) {
    this.isComputing_ = !0;
    var i = qt(!1), o;
    if (n)
      o = Un(this, this.derivation, this.scope_);
    else if (h.disableErrorBoundaries === !0)
      o = this.derivation.call(this.scope_);
    else
      try {
        o = this.derivation.call(this.scope_);
      } catch (a) {
        o = new Pt(a);
      }
    return Ht(i), this.isComputing_ = !1, o;
  }, t.suspend_ = function() {
    this.keepAlive_ || (cr(this), this.value_ = void 0, process.env.NODE_ENV !== "production" && this.isTracing_ !== M.NONE && console.log("[mobx.trace] Computed value '" + this.name_ + "' was suspended and it will recompute on the next access."));
  }, t.observe_ = function(n, i) {
    var o = this, a = !0, l = void 0;
    return Ar(function() {
      var s = o.get();
      if (!a || i) {
        var c = Ae();
        n({
          observableKind: "computed",
          debugObjectName: o.name_,
          type: k,
          object: o,
          newValue: s,
          oldValue: l
        }), Y(c);
      }
      a = !1, l = s;
    });
  }, t.warnAboutUntrackedRead_ = function() {
    process.env.NODE_ENV !== "production" && (this.isTracing_ !== M.NONE && console.log("[mobx.trace] Computed value '" + this.name_ + "' is being read outside a reactive context. Doing a full recompute."), (typeof this.requiresReaction_ == "boolean" ? this.requiresReaction_ : h.computedRequiresReaction) && console.warn("[mobx] Computed value '" + this.name_ + "' is being read outside a reactive context. Doing a full recompute."));
  }, t.toString = function() {
    return this.name_ + "[" + this.derivation.toString() + "]";
  }, t.valueOf = function() {
    return Nn(this.get());
  }, t[Mn] = function() {
    return this.valueOf();
  }, e;
}(), Ft = /* @__PURE__ */ Oe("ComputedValue", _e), b;
(function(e) {
  e[e.NOT_TRACKING_ = -1] = "NOT_TRACKING_", e[e.UP_TO_DATE_ = 0] = "UP_TO_DATE_", e[e.POSSIBLY_STALE_ = 1] = "POSSIBLY_STALE_", e[e.STALE_ = 2] = "STALE_";
})(b || (b = {}));
var M;
(function(e) {
  e[e.NONE = 0] = "NONE", e[e.LOG = 1] = "LOG", e[e.BREAK = 2] = "BREAK";
})(M || (M = {}));
var Pt = function(t) {
  this.cause = void 0, this.cause = t;
};
function Ot(e) {
  return e instanceof Pt;
}
function lr(e) {
  switch (e.dependenciesState_) {
    case b.UP_TO_DATE_:
      return !1;
    case b.NOT_TRACKING_:
    case b.STALE_:
      return !0;
    case b.POSSIBLY_STALE_: {
      for (var t = Er(!0), r = Ae(), n = e.observing_, i = n.length, o = 0; o < i; o++) {
        var a = n[o];
        if (Ft(a)) {
          if (h.disableErrorBoundaries)
            a.get();
          else
            try {
              a.get();
            } catch {
              return Y(r), qe(t), !0;
            }
          if (e.dependenciesState_ === b.STALE_)
            return Y(r), qe(t), !0;
        }
      }
      return Bn(e), Y(r), qe(t), !1;
    }
  }
}
function F(e) {
  if (process.env.NODE_ENV !== "production") {
    var t = e.observers_.size > 0;
    !h.allowStateChanges && (t || h.enforceActions === "always") && console.warn("[MobX] " + (h.enforceActions ? "Since strict-mode is enabled, changing (observed) observable values without using an action is not allowed. Tried to modify: " : "Side effects like changing state are not allowed at this point. Are you trying to modify state from, for example, a computed value or the render function of a React component? You can wrap side effects in 'runInAction' (or decorate functions with 'action') if needed. Tried to modify: ") + e.name_);
  }
}
function Jo(e) {
  process.env.NODE_ENV !== "production" && !h.allowStateReads && h.observableRequiresReaction && console.warn("[mobx] Observable '" + e.name_ + "' being read outside a reactive context.");
}
function Un(e, t, r) {
  var n = Er(!0);
  Bn(e), e.newObserving_ = new Array(
    // Reserve constant space for initial dependencies, dynamic space otherwise.
    // See https://github.com/mobxjs/mobx/pull/3833
    e.runId_ === 0 ? 100 : e.observing_.length
  ), e.unboundDepsCount_ = 0, e.runId_ = ++h.runId;
  var i = h.trackingDerivation;
  h.trackingDerivation = e, h.inBatch++;
  var o;
  if (h.disableErrorBoundaries === !0)
    o = t.call(r);
  else
    try {
      o = t.call(r);
    } catch (a) {
      o = new Pt(a);
    }
  return h.inBatch--, h.trackingDerivation = i, Zo(e), Xo(e), qe(n), o;
}
function Xo(e) {
  process.env.NODE_ENV !== "production" && e.observing_.length === 0 && (typeof e.requiresObservable_ == "boolean" ? e.requiresObservable_ : h.reactionRequiresObservable) && console.warn("[mobx] Derivation '" + e.name_ + "' is created/updated without reading any observable value.");
}
function Zo(e) {
  for (var t = e.observing_, r = e.observing_ = e.newObserving_, n = b.UP_TO_DATE_, i = 0, o = e.unboundDepsCount_, a = 0; a < o; a++) {
    var l = r[a];
    l.diffValue_ === 0 && (l.diffValue_ = 1, i !== a && (r[i] = l), i++), l.dependenciesState_ > n && (n = l.dependenciesState_);
  }
  for (r.length = i, e.newObserving_ = null, o = t.length; o--; ) {
    var s = t[o];
    s.diffValue_ === 0 && zn(s, e), s.diffValue_ = 0;
  }
  for (; i--; ) {
    var c = r[i];
    c.diffValue_ === 1 && (c.diffValue_ = 0, Yo(c, e));
  }
  n !== b.UP_TO_DATE_ && (e.dependenciesState_ = n, e.onBecomeStale_());
}
function cr(e) {
  var t = e.observing_;
  e.observing_ = [];
  for (var r = t.length; r--; )
    zn(t[r], e);
  e.dependenciesState_ = b.NOT_TRACKING_;
}
function kn(e) {
  var t = Ae();
  try {
    return e();
  } finally {
    Y(t);
  }
}
function Ae() {
  var e = h.trackingDerivation;
  return h.trackingDerivation = null, e;
}
function Y(e) {
  h.trackingDerivation = e;
}
function Er(e) {
  var t = h.allowStateReads;
  return h.allowStateReads = e, t;
}
function qe(e) {
  h.allowStateReads = e;
}
function Bn(e) {
  if (e.dependenciesState_ !== b.UP_TO_DATE_) {
    e.dependenciesState_ = b.UP_TO_DATE_;
    for (var t = e.observing_, r = t.length; r--; )
      t[r].lowestObserverState_ = b.UP_TO_DATE_;
  }
}
var Xt = function() {
  this.version = 6, this.UNCHANGED = {}, this.trackingDerivation = null, this.trackingContext = null, this.runId = 0, this.mobxGuid = 0, this.inBatch = 0, this.pendingUnobservations = [], this.pendingReactions = [], this.isRunningReactions = !1, this.allowStateChanges = !1, this.allowStateReads = !0, this.enforceActions = !0, this.spyListeners = [], this.globalReactionErrorHandlers = [], this.computedRequiresReaction = !1, this.reactionRequiresObservable = !1, this.observableRequiresReaction = !1, this.disableErrorBoundaries = !1, this.suppressReactionErrors = !1, this.useProxies = !0, this.verifyProxies = !1, this.safeDescriptors = !0;
}, Zt = !0, h = /* @__PURE__ */ function() {
  var e = /* @__PURE__ */ mn();
  return e.__mobxInstanceCount > 0 && !e.__mobxGlobals && (Zt = !1), e.__mobxGlobals && e.__mobxGlobals.version !== new Xt().version && (Zt = !1), Zt ? e.__mobxGlobals ? (e.__mobxInstanceCount += 1, e.__mobxGlobals.UNCHANGED || (e.__mobxGlobals.UNCHANGED = {}), e.__mobxGlobals) : (e.__mobxInstanceCount = 1, e.__mobxGlobals = /* @__PURE__ */ new Xt()) : (setTimeout(function() {
    v(35);
  }, 1), new Xt());
}();
function Yo(e, t) {
  e.observers_.add(t), e.lowestObserverState_ > t.dependenciesState_ && (e.lowestObserverState_ = t.dependenciesState_);
}
function zn(e, t) {
  e.observers_.delete(t), e.observers_.size === 0 && Kn(e);
}
function Kn(e) {
  e.isPendingUnobservation_ === !1 && (e.isPendingUnobservation_ = !0, h.pendingUnobservations.push(e));
}
function L() {
  h.inBatch++;
}
function I() {
  if (--h.inBatch === 0) {
    Wn();
    for (var e = h.pendingUnobservations, t = 0; t < e.length; t++) {
      var r = e[t];
      r.isPendingUnobservation_ = !1, r.observers_.size === 0 && (r.isBeingObserved_ && (r.isBeingObserved_ = !1, r.onBUO()), r instanceof _e && r.suspend_());
    }
    h.pendingUnobservations = [];
  }
}
function qn(e) {
  Jo(e);
  var t = h.trackingDerivation;
  return t !== null ? (t.runId_ !== e.lastAccessedBy_ && (e.lastAccessedBy_ = t.runId_, t.newObserving_[t.unboundDepsCount_++] = e, !e.isBeingObserved_ && h.trackingContext && (e.isBeingObserved_ = !0, e.onBO())), e.isBeingObserved_) : (e.observers_.size === 0 && h.inBatch > 0 && Kn(e), !1);
}
function Hn(e) {
  e.lowestObserverState_ !== b.STALE_ && (e.lowestObserverState_ = b.STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === b.UP_TO_DATE_ && (process.env.NODE_ENV !== "production" && t.isTracing_ !== M.NONE && Fn(t, e), t.onBecomeStale_()), t.dependenciesState_ = b.STALE_;
  }));
}
function Qo(e) {
  e.lowestObserverState_ !== b.STALE_ && (e.lowestObserverState_ = b.STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === b.POSSIBLY_STALE_ ? (t.dependenciesState_ = b.STALE_, process.env.NODE_ENV !== "production" && t.isTracing_ !== M.NONE && Fn(t, e)) : t.dependenciesState_ === b.UP_TO_DATE_ && (e.lowestObserverState_ = b.UP_TO_DATE_);
  }));
}
function ea(e) {
  e.lowestObserverState_ === b.UP_TO_DATE_ && (e.lowestObserverState_ = b.POSSIBLY_STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === b.UP_TO_DATE_ && (t.dependenciesState_ = b.POSSIBLY_STALE_, t.onBecomeStale_());
  }));
}
function Fn(e, t) {
  if (console.log("[mobx.trace] '" + e.name_ + "' is invalidated due to a change in: '" + t.name_ + "'"), e.isTracing_ === M.BREAK) {
    var r = [];
    Gn(pa(e), r, 1), new Function(`debugger;
/*
Tracing '` + e.name_ + `'

You are entering this break point because derivation '` + e.name_ + "' is being traced and '" + t.name_ + `' is now forcing it to update.
Just follow the stacktrace you should now see in the devtools to see precisely what piece of your code is causing this update
The stackframe you are looking for is at least ~6-8 stack-frames up.

` + (e instanceof _e ? e.derivation.toString().replace(/[*]\//g, "/") : "") + `

The dependencies for this derivation are:

` + r.join(`
`) + `
*/
    `)();
  }
}
function Gn(e, t, r) {
  if (t.length >= 1e3) {
    t.push("(and many more)");
    return;
  }
  t.push("" + "	".repeat(r - 1) + e.name), e.dependencies && e.dependencies.forEach(function(n) {
    return Gn(n, t, r + 1);
  });
}
var Je = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "Reaction@" + U() : "Reaction"), this.name_ = void 0, this.onInvalidate_ = void 0, this.errorHandler_ = void 0, this.requiresObservable_ = void 0, this.observing_ = [], this.newObserving_ = [], this.dependenciesState_ = b.NOT_TRACKING_, this.diffValue_ = 0, this.runId_ = 0, this.unboundDepsCount_ = 0, this.isDisposed_ = !1, this.isScheduled_ = !1, this.isTrackPending_ = !1, this.isRunning_ = !1, this.isTracing_ = M.NONE, this.name_ = r, this.onInvalidate_ = n, this.errorHandler_ = i, this.requiresObservable_ = o;
  }
  var t = e.prototype;
  return t.onBecomeStale_ = function() {
    this.schedule_();
  }, t.schedule_ = function() {
    this.isScheduled_ || (this.isScheduled_ = !0, h.pendingReactions.push(this), Wn());
  }, t.isScheduled = function() {
    return this.isScheduled_;
  }, t.runReaction_ = function() {
    if (!this.isDisposed_) {
      L(), this.isScheduled_ = !1;
      var n = h.trackingContext;
      if (h.trackingContext = this, lr(this)) {
        this.isTrackPending_ = !0;
        try {
          this.onInvalidate_(), process.env.NODE_ENV !== "production" && this.isTrackPending_ && $() && ge({
            name: this.name_,
            type: "scheduled-reaction"
          });
        } catch (i) {
          this.reportExceptionInDerivation_(i);
        }
      }
      h.trackingContext = n, I();
    }
  }, t.track = function(n) {
    if (!this.isDisposed_) {
      L();
      var i = $(), o;
      process.env.NODE_ENV !== "production" && i && (o = Date.now(), P({
        name: this.name_,
        type: "reaction"
      })), this.isRunning_ = !0;
      var a = h.trackingContext;
      h.trackingContext = this;
      var l = Un(this, n, void 0);
      h.trackingContext = a, this.isRunning_ = !1, this.isTrackPending_ = !1, this.isDisposed_ && cr(this), Ot(l) && this.reportExceptionInDerivation_(l.cause), process.env.NODE_ENV !== "production" && i && C({
        time: Date.now() - o
      }), I();
    }
  }, t.reportExceptionInDerivation_ = function(n) {
    var i = this;
    if (this.errorHandler_) {
      this.errorHandler_(n, this);
      return;
    }
    if (h.disableErrorBoundaries)
      throw n;
    var o = process.env.NODE_ENV !== "production" ? "[mobx] Encountered an uncaught exception that was thrown by a reaction or observer component, in: '" + this + "'" : "[mobx] uncaught error in '" + this + "'";
    h.suppressReactionErrors ? process.env.NODE_ENV !== "production" && console.warn("[mobx] (error in reaction '" + this.name_ + "' suppressed, fix error of causing action below)") : console.error(o, n), process.env.NODE_ENV !== "production" && $() && ge({
      type: "error",
      name: this.name_,
      message: o,
      error: "" + n
    }), h.globalReactionErrorHandlers.forEach(function(a) {
      return a(n, i);
    });
  }, t.dispose = function() {
    this.isDisposed_ || (this.isDisposed_ = !0, this.isRunning_ || (L(), cr(this), I()));
  }, t.getDisposer_ = function(n) {
    var i = this, o = function a() {
      i.dispose(), n == null || n.removeEventListener == null || n.removeEventListener("abort", a);
    };
    return n == null || n.addEventListener == null || n.addEventListener("abort", o), o[_] = this, o;
  }, t.toString = function() {
    return "Reaction[" + this.name_ + "]";
  }, t.trace = function(n) {
    n === void 0 && (n = !1), ya(this, n);
  }, e;
}(), Br = 100, ta = function(t) {
  return t();
};
function Wn() {
  h.inBatch > 0 || h.isRunningReactions || ta(ra);
}
function ra() {
  h.isRunningReactions = !0;
  for (var e = h.pendingReactions, t = 0; e.length > 0; ) {
    ++t === Br && (console.error(process.env.NODE_ENV !== "production" ? "Reaction doesn't converge to a stable state after " + Br + " iterations." + (" Probably there is a cycle in the reactive function: " + e[0]) : "[mobx] cycle in reaction: " + e[0]), e.splice(0));
    for (var r = e.splice(0), n = 0, i = r.length; n < i; n++)
      r[n].runReaction_();
  }
  h.isRunningReactions = !1;
}
var Ct = /* @__PURE__ */ Oe("Reaction", Je);
function $() {
  return process.env.NODE_ENV !== "production" && !!h.spyListeners.length;
}
function ge(e) {
  if (process.env.NODE_ENV !== "production" && h.spyListeners.length)
    for (var t = h.spyListeners, r = 0, n = t.length; r < n; r++)
      t[r](e);
}
function P(e) {
  if (process.env.NODE_ENV !== "production") {
    var t = ne({}, e, {
      spyReportStart: !0
    });
    ge(t);
  }
}
var na = {
  type: "report-end",
  spyReportEnd: !0
};
function C(e) {
  process.env.NODE_ENV !== "production" && ge(e ? ne({}, e, {
    type: "report-end",
    spyReportEnd: !0
  }) : na);
}
function ia(e) {
  return process.env.NODE_ENV === "production" ? (console.warn("[mobx.spy] Is a no-op in production builds"), function() {
  }) : (h.spyListeners.push(e), gr(function() {
    h.spyListeners = h.spyListeners.filter(function(t) {
      return t !== e;
    });
  }));
}
var Or = "action", oa = "action.bound", Jn = "autoAction", aa = "autoAction.bound", Xn = "<unnamed action>", ur = /* @__PURE__ */ lt(Or), sa = /* @__PURE__ */ lt(oa, {
  bound: !0
}), dr = /* @__PURE__ */ lt(Jn, {
  autoAction: !0
}), la = /* @__PURE__ */ lt(aa, {
  autoAction: !0,
  bound: !0
});
function Zn(e) {
  var t = function(n, i) {
    if (O(n))
      return pe(n.name || Xn, n, e);
    if (O(i))
      return pe(n, i, e);
    if (at(i))
      return (e ? dr : ur).decorate_20223_(n, i);
    if (ve(i))
      return Re(n, i, e ? dr : ur);
    if (ve(n))
      return W(lt(e ? Jn : Or, {
        name: n,
        autoAction: e
      }));
    process.env.NODE_ENV !== "production" && v("Invalid arguments for `action`");
  };
  return t;
}
var ce = /* @__PURE__ */ Zn(!1);
Object.assign(ce, ur);
var Xe = /* @__PURE__ */ Zn(!0);
Object.assign(Xe, dr);
ce.bound = /* @__PURE__ */ W(sa);
Xe.bound = /* @__PURE__ */ W(la);
function tl(e) {
  return Ln(e.name || Xn, !1, e, this, void 0);
}
function ct(e) {
  return O(e) && e.isMobxAction === !0;
}
function Ar(e, t) {
  var r, n, i, o, a;
  t === void 0 && (t = _r), process.env.NODE_ENV !== "production" && (O(e) || v("Autorun expects a function as first argument"), ct(e) && v("Autorun does not accept actions since actions are untrackable"));
  var l = (r = (n = t) == null ? void 0 : n.name) != null ? r : process.env.NODE_ENV !== "production" ? e.name || "Autorun@" + U() : "Autorun", s = !t.scheduler && !t.delay, c;
  if (s)
    c = new Je(l, function() {
      this.track(f);
    }, t.onError, t.requiresObservable);
  else {
    var u = Yn(t), d = !1;
    c = new Je(l, function() {
      d || (d = !0, u(function() {
        d = !1, c.isDisposed_ || c.track(f);
      }));
    }, t.onError, t.requiresObservable);
  }
  function f() {
    e(c);
  }
  return (i = t) != null && (o = i.signal) != null && o.aborted || c.schedule_(), c.getDisposer_((a = t) == null ? void 0 : a.signal);
}
var ca = function(t) {
  return t();
};
function Yn(e) {
  return e.scheduler ? e.scheduler : e.delay ? function(t) {
    return setTimeout(t, e.delay);
  } : ca;
}
function ua(e, t, r) {
  var n, i, o, a;
  r === void 0 && (r = _r), process.env.NODE_ENV !== "production" && ((!O(e) || !O(t)) && v("First and second argument to reaction should be functions"), x(r) || v("Third argument of reactions should be an object"));
  var l = (n = r.name) != null ? n : process.env.NODE_ENV !== "production" ? "Reaction@" + U() : "Reaction", s = ce(l, r.onError ? da(r.onError, t) : t), c = !r.scheduler && !r.delay, u = Yn(r), d = !0, f = !1, p, y = r.compareStructural ? Ce.structural : r.equals || Ce.default, m = new Je(l, function() {
    d || c ? N() : f || (f = !0, u(N));
  }, r.onError, r.requiresObservable);
  function N() {
    if (f = !1, !m.isDisposed_) {
      var H = !1, Ne = p;
      m.track(function() {
        var ee = Go(!1, function() {
          return e(m);
        });
        H = d || !y(p, ee), p = ee;
      }), (d && r.fireImmediately || !d && H) && s(p, Ne, m), d = !1;
    }
  }
  return (i = r) != null && (o = i.signal) != null && o.aborted || m.schedule_(), m.getDisposer_((a = r) == null ? void 0 : a.signal);
}
function da(e, t) {
  return function() {
    try {
      return t.apply(this, arguments);
    } catch (r) {
      e.call(this, r);
    }
  };
}
var ha = "onBO", va = "onBUO";
function fa(e, t, r) {
  return ei(ha, e, t, r);
}
function Qn(e, t, r) {
  return ei(va, e, t, r);
}
function ei(e, t, r, n) {
  var i = typeof n == "function" ? ie(t, r) : ie(t), o = O(n) ? n : r, a = e + "L";
  return i[a] ? i[a].add(o) : i[a] = /* @__PURE__ */ new Set([o]), function() {
    var l = i[a];
    l && (l.delete(o), l.size === 0 && delete i[a]);
  };
}
function ti(e, t, r, n) {
  process.env.NODE_ENV !== "production" && (arguments.length > 4 && v("'extendObservable' expected 2-4 arguments"), typeof e != "object" && v("'extendObservable' expects an object as first argument"), Q(e) && v("'extendObservable' should not be used on maps, use map.merge instead"), x(t) || v("'extendObservable' only accepts plain objects as second argument"), (Ye(t) || Ye(r)) && v("Extending an object with another observable (object) is not supported"));
  var i = Zi(t);
  return we(function() {
    var o = Le(e, n)[_];
    We(i).forEach(function(a) {
      o.extend_(
        a,
        i[a],
        // must pass "undefined" for { key: undefined }
        r && a in r ? r[a] : !0
      );
    });
  }), e;
}
function pa(e, t) {
  return ri(ie(e, t));
}
function ri(e) {
  var t = {
    name: e.name_
  };
  return e.observing_ && e.observing_.length > 0 && (t.dependencies = _a(e.observing_).map(ri)), t;
}
function _a(e) {
  return Array.from(new Set(e));
}
var ga = 0;
function ni() {
  this.message = "FLOW_CANCELLED";
}
ni.prototype = /* @__PURE__ */ Object.create(Error.prototype);
var Yt = /* @__PURE__ */ Dn("flow"), ba = /* @__PURE__ */ Dn("flow.bound", {
  bound: !0
}), Ve = /* @__PURE__ */ Object.assign(function(t, r) {
  if (at(r))
    return Yt.decorate_20223_(t, r);
  if (ve(r))
    return Re(t, r, Yt);
  process.env.NODE_ENV !== "production" && arguments.length !== 1 && v("Flow expects single argument with generator function");
  var n = t, i = n.name || "<unnamed flow>", o = function() {
    var l = this, s = arguments, c = ++ga, u = ce(i + " - runid: " + c + " - init", n).apply(l, s), d, f = void 0, p = new Promise(function(y, m) {
      var N = 0;
      d = m;
      function H(D) {
        f = void 0;
        var te;
        try {
          te = ce(i + " - runid: " + c + " - yield " + N++, u.next).call(u, D);
        } catch (oe) {
          return m(oe);
        }
        ee(te);
      }
      function Ne(D) {
        f = void 0;
        var te;
        try {
          te = ce(i + " - runid: " + c + " - yield " + N++, u.throw).call(u, D);
        } catch (oe) {
          return m(oe);
        }
        ee(te);
      }
      function ee(D) {
        if (O(D?.then)) {
          D.then(ee, m);
          return;
        }
        return D.done ? y(D.value) : (f = Promise.resolve(D.value), f.then(H, Ne));
      }
      H(void 0);
    });
    return p.cancel = ce(i + " - runid: " + c + " - cancel", function() {
      try {
        f && zr(f);
        var y = u.return(void 0), m = Promise.resolve(y.value);
        m.then(De, De), zr(m), d(new ni());
      } catch (N) {
        d(N);
      }
    }), p;
  };
  return o.isMobXFlow = !0, o;
}, Yt);
Ve.bound = /* @__PURE__ */ W(ba);
function zr(e) {
  O(e.cancel) && e.cancel();
}
function Ze(e) {
  return e?.isMobXFlow === !0;
}
function ma(e, t) {
  return e ? t !== void 0 ? process.env.NODE_ENV !== "production" && (Q(e) || ht(e)) ? v("isObservable(object, propertyName) is not supported for arrays and maps. Use map.has or array.length instead.") : be(e) ? e[_].values_.has(t) : !1 : be(e) || !!e[_] || mr(e) || Ct(e) || Ft(e) : !1;
}
function Ye(e) {
  return process.env.NODE_ENV !== "production" && arguments.length !== 1 && v("isObservable expects only 1 argument. Use isObservableProp to inspect the observability of a property"), ma(e);
}
function ya() {
  if (process.env.NODE_ENV !== "production") {
    for (var e = !1, t = arguments.length, r = new Array(t), n = 0; n < t; n++)
      r[n] = arguments[n];
    typeof r[r.length - 1] == "boolean" && (e = r.pop());
    var i = Ea(r);
    if (!i)
      return v("'trace(break?)' can only be used inside a tracked computed value or a Reaction. Consider passing in the computed value or reaction explicitly");
    i.isTracing_ === M.NONE && console.log("[mobx.trace] '" + i.name_ + "' tracing enabled"), i.isTracing_ = e ? M.BREAK : M.LOG;
  }
}
function Ea(e) {
  switch (e.length) {
    case 0:
      return h.trackingDerivation;
    case 1:
      return ie(e[0]);
    case 2:
      return ie(e[0], e[1]);
  }
}
function X(e, t) {
  t === void 0 && (t = void 0), L();
  try {
    return e.apply(t);
  } finally {
    I();
  }
}
function ae(e) {
  return e[_];
}
var Oa = {
  has: function(t, r) {
    return process.env.NODE_ENV !== "production" && h.trackingDerivation && Me("detect new properties using the 'in' operator. Use 'has' from 'mobx' instead."), ae(t).has_(r);
  },
  get: function(t, r) {
    return ae(t).get_(r);
  },
  set: function(t, r, n) {
    var i;
    return ve(r) ? (process.env.NODE_ENV !== "production" && !ae(t).values_.has(r) && Me("add a new observable property through direct assignment. Use 'set' from 'mobx' instead."), (i = ae(t).set_(r, n, !0)) != null ? i : !0) : !1;
  },
  deleteProperty: function(t, r) {
    var n;
    return process.env.NODE_ENV !== "production" && Me("delete properties from an observable object. Use 'remove' from 'mobx' instead."), ve(r) ? (n = ae(t).delete_(r, !0)) != null ? n : !0 : !1;
  },
  defineProperty: function(t, r, n) {
    var i;
    return process.env.NODE_ENV !== "production" && Me("define property on an observable object. Use 'defineProperty' from 'mobx' instead."), (i = ae(t).defineProperty_(r, n)) != null ? i : !0;
  },
  ownKeys: function(t) {
    return process.env.NODE_ENV !== "production" && h.trackingDerivation && Me("iterate keys to detect added / removed properties. Use 'keys' from 'mobx' instead."), ae(t).ownKeys_();
  },
  preventExtensions: function(t) {
    v(13);
  }
};
function Aa(e, t) {
  var r, n;
  return En(), e = Le(e, t), (n = (r = e[_]).proxy_) != null ? n : r.proxy_ = new Proxy(e, Oa);
}
function R(e) {
  return e.interceptors_ !== void 0 && e.interceptors_.length > 0;
}
function ut(e, t) {
  var r = e.interceptors_ || (e.interceptors_ = []);
  return r.push(t), gr(function() {
    var n = r.indexOf(t);
    n !== -1 && r.splice(n, 1);
  });
}
function j(e, t) {
  var r = Ae();
  try {
    for (var n = [].concat(e.interceptors_ || []), i = 0, o = n.length; i < o && (t = n[i](t), t && !t.type && v(14), !!t); i++)
      ;
    return t;
  } finally {
    Y(r);
  }
}
function z(e) {
  return e.changeListeners_ !== void 0 && e.changeListeners_.length > 0;
}
function dt(e, t) {
  var r = e.changeListeners_ || (e.changeListeners_ = []);
  return r.push(t), gr(function() {
    var n = r.indexOf(t);
    n !== -1 && r.splice(n, 1);
  });
}
function K(e, t) {
  var r = Ae(), n = e.changeListeners_;
  if (n) {
    n = n.slice();
    for (var i = 0, o = n.length; i < o; i++)
      n[i](t);
    Y(r);
  }
}
var Qt = /* @__PURE__ */ Symbol("mobx-keys");
function wr(e, t, r) {
  return process.env.NODE_ENV !== "production" && (!x(e) && !x(Object.getPrototypeOf(e)) && v("'makeAutoObservable' can only be used for classes that don't have a superclass"), be(e) && v("makeAutoObservable can only be used on objects not already made observable")), x(e) ? ti(e, e, t, r) : (we(function() {
    var n = Le(e, r)[_];
    if (!e[Qt]) {
      var i = Object.getPrototypeOf(e), o = new Set([].concat(We(e), We(i)));
      o.delete("constructor"), o.delete(_), Ut(i, Qt, o);
    }
    e[Qt].forEach(function(a) {
      return n.make_(
        a,
        // must pass "undefined" for { key: undefined }
        t && a in t ? t[a] : !0
      );
    });
  }), e);
}
var Kr = "splice", k = "update", wa = 1e4, Na = {
  get: function(t, r) {
    var n = t[_];
    return r === _ ? n : r === "length" ? n.getArrayLength_() : typeof r == "string" && !isNaN(r) ? n.get_(parseInt(r)) : B(Vt, r) ? Vt[r] : t[r];
  },
  set: function(t, r, n) {
    var i = t[_];
    return r === "length" && i.setArrayLength_(n), typeof r == "symbol" || isNaN(r) ? t[r] = n : i.set_(parseInt(r), n), !0;
  },
  preventExtensions: function() {
    v(15);
  }
}, Nr = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "ObservableArray@" + U() : "ObservableArray"), this.owned_ = void 0, this.legacyMode_ = void 0, this.atom_ = void 0, this.values_ = [], this.interceptors_ = void 0, this.changeListeners_ = void 0, this.enhancer_ = void 0, this.dehancer = void 0, this.proxy_ = void 0, this.lastKnownLength_ = 0, this.owned_ = i, this.legacyMode_ = o, this.atom_ = new st(r), this.enhancer_ = function(a, l) {
      return n(a, l, process.env.NODE_ENV !== "production" ? r + "[..]" : "ObservableArray[..]");
    };
  }
  var t = e.prototype;
  return t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.dehanceValues_ = function(n) {
    return this.dehancer !== void 0 && n.length > 0 ? n.map(this.dehancer) : n;
  }, t.intercept_ = function(n) {
    return ut(this, n);
  }, t.observe_ = function(n, i) {
    return i === void 0 && (i = !1), i && n({
      observableKind: "array",
      object: this.proxy_,
      debugObjectName: this.atom_.name_,
      type: "splice",
      index: 0,
      added: this.values_.slice(),
      addedCount: this.values_.length,
      removed: [],
      removedCount: 0
    }), dt(this, n);
  }, t.getArrayLength_ = function() {
    return this.atom_.reportObserved(), this.values_.length;
  }, t.setArrayLength_ = function(n) {
    (typeof n != "number" || isNaN(n) || n < 0) && v("Out of range: " + n);
    var i = this.values_.length;
    if (n !== i)
      if (n > i) {
        for (var o = new Array(n - i), a = 0; a < n - i; a++)
          o[a] = void 0;
        this.spliceWithArray_(i, 0, o);
      } else
        this.spliceWithArray_(n, i - n);
  }, t.updateArrayLength_ = function(n, i) {
    n !== this.lastKnownLength_ && v(16), this.lastKnownLength_ += i, this.legacyMode_ && i > 0 && vi(n + i + 1);
  }, t.spliceWithArray_ = function(n, i, o) {
    var a = this;
    F(this.atom_);
    var l = this.values_.length;
    if (n === void 0 ? n = 0 : n > l ? n = l : n < 0 && (n = Math.max(0, l + n)), arguments.length === 1 ? i = l - n : i == null ? i = 0 : i = Math.max(0, Math.min(i, l - n)), o === void 0 && (o = $t), R(this)) {
      var s = j(this, {
        object: this.proxy_,
        type: Kr,
        index: n,
        removedCount: i,
        added: o
      });
      if (!s)
        return $t;
      i = s.removedCount, o = s.added;
    }
    if (o = o.length === 0 ? o : o.map(function(d) {
      return a.enhancer_(d, void 0);
    }), this.legacyMode_ || process.env.NODE_ENV !== "production") {
      var c = o.length - i;
      this.updateArrayLength_(l, c);
    }
    var u = this.spliceItemsIntoValues_(n, i, o);
    return (i !== 0 || o.length !== 0) && this.notifyArraySplice_(n, o, u), this.dehanceValues_(u);
  }, t.spliceItemsIntoValues_ = function(n, i, o) {
    if (o.length < wa) {
      var a;
      return (a = this.values_).splice.apply(a, [n, i].concat(o));
    } else {
      var l = this.values_.slice(n, n + i), s = this.values_.slice(n + i);
      this.values_.length += o.length - i;
      for (var c = 0; c < o.length; c++)
        this.values_[n + c] = o[c];
      for (var u = 0; u < s.length; u++)
        this.values_[n + o.length + u] = s[u];
      return l;
    }
  }, t.notifyArrayChildUpdate_ = function(n, i, o) {
    var a = !this.owned_ && $(), l = z(this), s = l || a ? {
      observableKind: "array",
      object: this.proxy_,
      type: k,
      debugObjectName: this.atom_.name_,
      index: n,
      newValue: i,
      oldValue: o
    } : null;
    process.env.NODE_ENV !== "production" && a && P(s), this.atom_.reportChanged(), l && K(this, s), process.env.NODE_ENV !== "production" && a && C();
  }, t.notifyArraySplice_ = function(n, i, o) {
    var a = !this.owned_ && $(), l = z(this), s = l || a ? {
      observableKind: "array",
      object: this.proxy_,
      debugObjectName: this.atom_.name_,
      type: Kr,
      index: n,
      removed: o,
      added: i,
      removedCount: o.length,
      addedCount: i.length
    } : null;
    process.env.NODE_ENV !== "production" && a && P(s), this.atom_.reportChanged(), l && K(this, s), process.env.NODE_ENV !== "production" && a && C();
  }, t.get_ = function(n) {
    if (this.legacyMode_ && n >= this.values_.length) {
      console.warn(process.env.NODE_ENV !== "production" ? "[mobx.array] Attempt to read an array index (" + n + ") that is out of bounds (" + this.values_.length + "). Please check length first. Out of bound indices will not be tracked by MobX" : "[mobx] Out of bounds read: " + n);
      return;
    }
    return this.atom_.reportObserved(), this.dehanceValue_(this.values_[n]);
  }, t.set_ = function(n, i) {
    var o = this.values_;
    if (this.legacyMode_ && n > o.length && v(17, n, o.length), n < o.length) {
      F(this.atom_);
      var a = o[n];
      if (R(this)) {
        var l = j(this, {
          type: k,
          object: this.proxy_,
          index: n,
          newValue: i
        });
        if (!l)
          return;
        i = l.newValue;
      }
      i = this.enhancer_(i, a);
      var s = i !== a;
      s && (o[n] = i, this.notifyArrayChildUpdate_(n, i, a));
    } else {
      for (var c = new Array(n + 1 - o.length), u = 0; u < c.length - 1; u++)
        c[u] = void 0;
      c[c.length - 1] = i, this.spliceWithArray_(o.length, 0, c);
    }
  }, e;
}();
function Sa(e, t, r, n) {
  return r === void 0 && (r = process.env.NODE_ENV !== "production" ? "ObservableArray@" + U() : "ObservableArray"), n === void 0 && (n = !1), En(), we(function() {
    var i = new Nr(r, t, n, !1);
    An(i.values_, _, i);
    var o = new Proxy(i.values_, Na);
    return i.proxy_ = o, e && e.length && i.spliceWithArray_(0, 0, e), o;
  });
}
var Vt = {
  clear: function() {
    return this.splice(0);
  },
  replace: function(t) {
    var r = this[_];
    return r.spliceWithArray_(0, r.values_.length, t);
  },
  // Used by JSON.stringify
  toJSON: function() {
    return this.slice();
  },
  /*
   * functions that do alter the internal structure of the array, (based on lib.es6.d.ts)
   * since these functions alter the inner structure of the array, the have side effects.
   * Because the have side effects, they should not be used in computed function,
   * and for that reason the do not call dependencyState.notifyObserved
   */
  splice: function(t, r) {
    for (var n = arguments.length, i = new Array(n > 2 ? n - 2 : 0), o = 2; o < n; o++)
      i[o - 2] = arguments[o];
    var a = this[_];
    switch (arguments.length) {
      case 0:
        return [];
      case 1:
        return a.spliceWithArray_(t);
      case 2:
        return a.spliceWithArray_(t, r);
    }
    return a.spliceWithArray_(t, r, i);
  },
  spliceWithArray: function(t, r, n) {
    return this[_].spliceWithArray_(t, r, n);
  },
  push: function() {
    for (var t = this[_], r = arguments.length, n = new Array(r), i = 0; i < r; i++)
      n[i] = arguments[i];
    return t.spliceWithArray_(t.values_.length, 0, n), t.values_.length;
  },
  pop: function() {
    return this.splice(Math.max(this[_].values_.length - 1, 0), 1)[0];
  },
  shift: function() {
    return this.splice(0, 1)[0];
  },
  unshift: function() {
    for (var t = this[_], r = arguments.length, n = new Array(r), i = 0; i < r; i++)
      n[i] = arguments[i];
    return t.spliceWithArray_(0, 0, n), t.values_.length;
  },
  reverse: function() {
    return h.trackingDerivation && v(37, "reverse"), this.replace(this.slice().reverse()), this;
  },
  sort: function() {
    h.trackingDerivation && v(37, "sort");
    var t = this.slice();
    return t.sort.apply(t, arguments), this.replace(t), this;
  },
  remove: function(t) {
    var r = this[_], n = r.dehanceValues_(r.values_).indexOf(t);
    return n > -1 ? (this.splice(n, 1), !0) : !1;
  }
};
E("at", V);
E("concat", V);
E("flat", V);
E("includes", V);
E("indexOf", V);
E("join", V);
E("lastIndexOf", V);
E("slice", V);
E("toString", V);
E("toLocaleString", V);
E("toSorted", V);
E("toSpliced", V);
E("with", V);
E("every", q);
E("filter", q);
E("find", q);
E("findIndex", q);
E("findLast", q);
E("findLastIndex", q);
E("flatMap", q);
E("forEach", q);
E("map", q);
E("some", q);
E("toReversed", q);
E("reduce", ii);
E("reduceRight", ii);
function E(e, t) {
  typeof Array.prototype[e] == "function" && (Vt[e] = t(e));
}
function V(e) {
  return function() {
    var t = this[_];
    t.atom_.reportObserved();
    var r = t.dehanceValues_(t.values_);
    return r[e].apply(r, arguments);
  };
}
function q(e) {
  return function(t, r) {
    var n = this, i = this[_];
    i.atom_.reportObserved();
    var o = i.dehanceValues_(i.values_);
    return o[e](function(a, l) {
      return t.call(r, a, l, n);
    });
  };
}
function ii(e) {
  return function() {
    var t = this, r = this[_];
    r.atom_.reportObserved();
    var n = r.dehanceValues_(r.values_), i = arguments[0];
    return arguments[0] = function(o, a, l) {
      return i(o, a, l, t);
    }, n[e].apply(n, arguments);
  };
}
var $a = /* @__PURE__ */ Oe("ObservableArrayAdministration", Nr);
function ht(e) {
  return Mt(e) && $a(e[_]);
}
var oi, ai, xa = {}, re = "add", Tt = "delete";
oi = Symbol.iterator;
ai = Symbol.toStringTag;
var si = /* @__PURE__ */ function() {
  function e(r, n, i) {
    var o = this;
    n === void 0 && (n = fe), i === void 0 && (i = process.env.NODE_ENV !== "production" ? "ObservableMap@" + U() : "ObservableMap"), this.enhancer_ = void 0, this.name_ = void 0, this[_] = xa, this.data_ = void 0, this.hasMap_ = void 0, this.keysAtom_ = void 0, this.interceptors_ = void 0, this.changeListeners_ = void 0, this.dehancer = void 0, this.enhancer_ = n, this.name_ = i, O(Map) || v(18), we(function() {
      o.keysAtom_ = $n(process.env.NODE_ENV !== "production" ? o.name_ + ".keys()" : "ObservableMap.keys()"), o.data_ = /* @__PURE__ */ new Map(), o.hasMap_ = /* @__PURE__ */ new Map(), r && o.merge(r);
    });
  }
  var t = e.prototype;
  return t.has_ = function(n) {
    return this.data_.has(n);
  }, t.has = function(n) {
    var i = this;
    if (!h.trackingDerivation)
      return this.has_(n);
    var o = this.hasMap_.get(n);
    if (!o) {
      var a = o = new de(this.has_(n), Bt, process.env.NODE_ENV !== "production" ? this.name_ + "." + ir(n) + "?" : "ObservableMap.key?", !1);
      this.hasMap_.set(n, a), Qn(a, function() {
        return i.hasMap_.delete(n);
      });
    }
    return o.get();
  }, t.set = function(n, i) {
    var o = this.has_(n);
    if (R(this)) {
      var a = j(this, {
        type: o ? k : re,
        object: this,
        newValue: i,
        name: n
      });
      if (!a)
        return this;
      i = a.newValue;
    }
    return o ? this.updateValue_(n, i) : this.addValue_(n, i), this;
  }, t.delete = function(n) {
    var i = this;
    if (F(this.keysAtom_), R(this)) {
      var o = j(this, {
        type: Tt,
        object: this,
        name: n
      });
      if (!o)
        return !1;
    }
    if (this.has_(n)) {
      var a = $(), l = z(this), s = l || a ? {
        observableKind: "map",
        debugObjectName: this.name_,
        type: Tt,
        object: this,
        oldValue: this.data_.get(n).value_,
        name: n
      } : null;
      return process.env.NODE_ENV !== "production" && a && P(s), X(function() {
        var c;
        i.keysAtom_.reportChanged(), (c = i.hasMap_.get(n)) == null || c.setNewValue_(!1);
        var u = i.data_.get(n);
        u.setNewValue_(void 0), i.data_.delete(n);
      }), l && K(this, s), process.env.NODE_ENV !== "production" && a && C(), !0;
    }
    return !1;
  }, t.updateValue_ = function(n, i) {
    var o = this.data_.get(n);
    if (i = o.prepareNewValue_(i), i !== h.UNCHANGED) {
      var a = $(), l = z(this), s = l || a ? {
        observableKind: "map",
        debugObjectName: this.name_,
        type: k,
        object: this,
        oldValue: o.value_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && a && P(s), o.setNewValue_(i), l && K(this, s), process.env.NODE_ENV !== "production" && a && C();
    }
  }, t.addValue_ = function(n, i) {
    var o = this;
    F(this.keysAtom_), X(function() {
      var c, u = new de(i, o.enhancer_, process.env.NODE_ENV !== "production" ? o.name_ + "." + ir(n) : "ObservableMap.key", !1);
      o.data_.set(n, u), i = u.value_, (c = o.hasMap_.get(n)) == null || c.setNewValue_(!0), o.keysAtom_.reportChanged();
    });
    var a = $(), l = z(this), s = l || a ? {
      observableKind: "map",
      debugObjectName: this.name_,
      type: re,
      object: this,
      name: n,
      newValue: i
    } : null;
    process.env.NODE_ENV !== "production" && a && P(s), l && K(this, s), process.env.NODE_ENV !== "production" && a && C();
  }, t.get = function(n) {
    return this.has(n) ? this.dehanceValue_(this.data_.get(n).get()) : this.dehanceValue_(void 0);
  }, t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.keys = function() {
    return this.keysAtom_.reportObserved(), this.data_.keys();
  }, t.values = function() {
    var n = this, i = this.keys();
    return Qe({
      next: function() {
        var a = i.next(), l = a.done, s = a.value;
        return {
          done: l,
          value: l ? void 0 : n.get(s)
        };
      }
    });
  }, t.entries = function() {
    var n = this, i = this.keys();
    return Qe({
      next: function() {
        var a = i.next(), l = a.done, s = a.value;
        return {
          done: l,
          value: l ? void 0 : [s, n.get(s)]
        };
      }
    });
  }, t[oi] = function() {
    return this.entries();
  }, t.forEach = function(n, i) {
    for (var o = Pe(this), a; !(a = o()).done; ) {
      var l = a.value, s = l[0], c = l[1];
      n.call(i, c, s, this);
    }
  }, t.merge = function(n) {
    var i = this;
    return Q(n) && (n = new Map(n)), X(function() {
      x(n) ? Xi(n).forEach(function(o) {
        return i.set(o, n[o]);
      }) : Array.isArray(n) ? n.forEach(function(o) {
        var a = o[0], l = o[1];
        return i.set(a, l);
      }) : Te(n) ? (n.constructor !== Map && v(19, n), n.forEach(function(o, a) {
        return i.set(a, o);
      })) : n != null && v(20, n);
    }), this;
  }, t.clear = function() {
    var n = this;
    X(function() {
      kn(function() {
        for (var i = Pe(n.keys()), o; !(o = i()).done; ) {
          var a = o.value;
          n.delete(a);
        }
      });
    });
  }, t.replace = function(n) {
    var i = this;
    return X(function() {
      for (var o = Da(n), a = /* @__PURE__ */ new Map(), l = !1, s = Pe(i.data_.keys()), c; !(c = s()).done; ) {
        var u = c.value;
        if (!o.has(u)) {
          var d = i.delete(u);
          if (d)
            l = !0;
          else {
            var f = i.data_.get(u);
            a.set(u, f);
          }
        }
      }
      for (var p = Pe(o.entries()), y; !(y = p()).done; ) {
        var m = y.value, N = m[0], H = m[1], Ne = i.data_.has(N);
        if (i.set(N, H), i.data_.has(N)) {
          var ee = i.data_.get(N);
          a.set(N, ee), Ne || (l = !0);
        }
      }
      if (!l)
        if (i.data_.size !== a.size)
          i.keysAtom_.reportChanged();
        else
          for (var D = i.data_.keys(), te = a.keys(), oe = D.next(), jr = te.next(); !oe.done; ) {
            if (oe.value !== jr.value) {
              i.keysAtom_.reportChanged();
              break;
            }
            oe = D.next(), jr = te.next();
          }
      i.data_ = a;
    }), this;
  }, t.toString = function() {
    return "[object ObservableMap]";
  }, t.toJSON = function() {
    return Array.from(this);
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support fireImmediately=true in combination with maps."), dt(this, n);
  }, t.intercept_ = function(n) {
    return ut(this, n);
  }, br(e, [{
    key: "size",
    get: function() {
      return this.keysAtom_.reportObserved(), this.data_.size;
    }
  }, {
    key: ai,
    get: function() {
      return "Map";
    }
  }]), e;
}(), Q = /* @__PURE__ */ Oe("ObservableMap", si);
function Da(e) {
  if (Te(e) || Q(e))
    return e;
  if (Array.isArray(e))
    return new Map(e);
  if (x(e)) {
    var t = /* @__PURE__ */ new Map();
    for (var r in e)
      t.set(r, e[r]);
    return t;
  } else
    return v(21, e);
}
var li, ci, Pa = {};
li = Symbol.iterator;
ci = Symbol.toStringTag;
var ui = /* @__PURE__ */ function() {
  function e(r, n, i) {
    var o = this;
    n === void 0 && (n = fe), i === void 0 && (i = process.env.NODE_ENV !== "production" ? "ObservableSet@" + U() : "ObservableSet"), this.name_ = void 0, this[_] = Pa, this.data_ = /* @__PURE__ */ new Set(), this.atom_ = void 0, this.changeListeners_ = void 0, this.interceptors_ = void 0, this.dehancer = void 0, this.enhancer_ = void 0, this.name_ = i, O(Set) || v(22), this.enhancer_ = function(a, l) {
      return n(a, l, i);
    }, we(function() {
      o.atom_ = $n(o.name_), r && o.replace(r);
    });
  }
  var t = e.prototype;
  return t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.clear = function() {
    var n = this;
    X(function() {
      kn(function() {
        for (var i = Pe(n.data_.values()), o; !(o = i()).done; ) {
          var a = o.value;
          n.delete(a);
        }
      });
    });
  }, t.forEach = function(n, i) {
    for (var o = Pe(this), a; !(a = o()).done; ) {
      var l = a.value;
      n.call(i, l, l, this);
    }
  }, t.add = function(n) {
    var i = this;
    if (F(this.atom_), R(this)) {
      var o = j(this, {
        type: re,
        object: this,
        newValue: n
      });
      if (!o)
        return this;
    }
    if (!this.has(n)) {
      X(function() {
        i.data_.add(i.enhancer_(n, void 0)), i.atom_.reportChanged();
      });
      var a = process.env.NODE_ENV !== "production" && $(), l = z(this), s = l || a ? {
        observableKind: "set",
        debugObjectName: this.name_,
        type: re,
        object: this,
        newValue: n
      } : null;
      a && process.env.NODE_ENV !== "production" && P(s), l && K(this, s), a && process.env.NODE_ENV !== "production" && C();
    }
    return this;
  }, t.delete = function(n) {
    var i = this;
    if (R(this)) {
      var o = j(this, {
        type: Tt,
        object: this,
        oldValue: n
      });
      if (!o)
        return !1;
    }
    if (this.has(n)) {
      var a = process.env.NODE_ENV !== "production" && $(), l = z(this), s = l || a ? {
        observableKind: "set",
        debugObjectName: this.name_,
        type: Tt,
        object: this,
        oldValue: n
      } : null;
      return a && process.env.NODE_ENV !== "production" && P(s), X(function() {
        i.atom_.reportChanged(), i.data_.delete(n);
      }), l && K(this, s), a && process.env.NODE_ENV !== "production" && C(), !0;
    }
    return !1;
  }, t.has = function(n) {
    return this.atom_.reportObserved(), this.data_.has(this.dehanceValue_(n));
  }, t.entries = function() {
    var n = 0, i = Array.from(this.keys()), o = Array.from(this.values());
    return Qe({
      next: function() {
        var l = n;
        return n += 1, l < o.length ? {
          value: [i[l], o[l]],
          done: !1
        } : {
          done: !0
        };
      }
    });
  }, t.keys = function() {
    return this.values();
  }, t.values = function() {
    this.atom_.reportObserved();
    var n = this, i = 0, o = Array.from(this.data_.values());
    return Qe({
      next: function() {
        return i < o.length ? {
          value: n.dehanceValue_(o[i++]),
          done: !1
        } : {
          done: !0
        };
      }
    });
  }, t.replace = function(n) {
    var i = this;
    return je(n) && (n = new Set(n)), X(function() {
      Array.isArray(n) ? (i.clear(), n.forEach(function(o) {
        return i.add(o);
      })) : ot(n) ? (i.clear(), n.forEach(function(o) {
        return i.add(o);
      })) : n != null && v("Cannot initialize set from " + n);
    }), this;
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support fireImmediately=true in combination with sets."), dt(this, n);
  }, t.intercept_ = function(n) {
    return ut(this, n);
  }, t.toJSON = function() {
    return Array.from(this);
  }, t.toString = function() {
    return "[object ObservableSet]";
  }, t[li] = function() {
    return this.values();
  }, br(e, [{
    key: "size",
    get: function() {
      return this.atom_.reportObserved(), this.data_.size;
    }
  }, {
    key: ci,
    get: function() {
      return "Set";
    }
  }]), e;
}(), je = /* @__PURE__ */ Oe("ObservableSet", ui), qr = /* @__PURE__ */ Object.create(null), Hr = "remove", hr = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    n === void 0 && (n = /* @__PURE__ */ new Map()), o === void 0 && (o = $o), this.target_ = void 0, this.values_ = void 0, this.name_ = void 0, this.defaultAnnotation_ = void 0, this.keysAtom_ = void 0, this.changeListeners_ = void 0, this.interceptors_ = void 0, this.proxy_ = void 0, this.isPlainObject_ = void 0, this.appliedAnnotations_ = void 0, this.pendingKeys_ = void 0, this.target_ = r, this.values_ = n, this.name_ = i, this.defaultAnnotation_ = o, this.keysAtom_ = new st(process.env.NODE_ENV !== "production" ? this.name_ + ".keys" : "ObservableObject.keys"), this.isPlainObject_ = x(this.target_), process.env.NODE_ENV !== "production" && !fi(this.defaultAnnotation_) && v("defaultAnnotation must be valid annotation"), process.env.NODE_ENV !== "production" && (this.appliedAnnotations_ = {});
  }
  var t = e.prototype;
  return t.getObservablePropValue_ = function(n) {
    return this.values_.get(n).get();
  }, t.setObservablePropValue_ = function(n, i) {
    var o = this.values_.get(n);
    if (o instanceof _e)
      return o.set(i), !0;
    if (R(this)) {
      var a = j(this, {
        type: k,
        object: this.proxy_ || this.target_,
        name: n,
        newValue: i
      });
      if (!a)
        return null;
      i = a.newValue;
    }
    if (i = o.prepareNewValue_(i), i !== h.UNCHANGED) {
      var l = z(this), s = process.env.NODE_ENV !== "production" && $(), c = l || s ? {
        type: k,
        observableKind: "object",
        debugObjectName: this.name_,
        object: this.proxy_ || this.target_,
        oldValue: o.value_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && s && P(c), o.setNewValue_(i), l && K(this, c), process.env.NODE_ENV !== "production" && s && C();
    }
    return !0;
  }, t.get_ = function(n) {
    return h.trackingDerivation && !B(this.target_, n) && this.has_(n), this.target_[n];
  }, t.set_ = function(n, i, o) {
    return o === void 0 && (o = !1), B(this.target_, n) ? this.values_.has(n) ? this.setObservablePropValue_(n, i) : o ? Reflect.set(this.target_, n, i) : (this.target_[n] = i, !0) : this.extend_(n, {
      value: i,
      enumerable: !0,
      writable: !0,
      configurable: !0
    }, this.defaultAnnotation_, o);
  }, t.has_ = function(n) {
    if (!h.trackingDerivation)
      return n in this.target_;
    this.pendingKeys_ || (this.pendingKeys_ = /* @__PURE__ */ new Map());
    var i = this.pendingKeys_.get(n);
    return i || (i = new de(n in this.target_, Bt, process.env.NODE_ENV !== "production" ? this.name_ + "." + ir(n) + "?" : "ObservableObject.key?", !1), this.pendingKeys_.set(n, i)), i.get();
  }, t.make_ = function(n, i) {
    if (i === !0 && (i = this.defaultAnnotation_), i !== !1) {
      if (Wr(this, i, n), !(n in this.target_)) {
        var o;
        if ((o = this.target_[J]) != null && o[n])
          return;
        v(1, i.annotationType_, this.name_ + "." + n.toString());
      }
      for (var a = this.target_; a && a !== It; ) {
        var l = St(a, n);
        if (l) {
          var s = i.make_(this, n, l, a);
          if (s === 0)
            return;
          if (s === 1)
            break;
        }
        a = Object.getPrototypeOf(a);
      }
      Gr(this, i, n);
    }
  }, t.extend_ = function(n, i, o, a) {
    if (a === void 0 && (a = !1), o === !0 && (o = this.defaultAnnotation_), o === !1)
      return this.defineProperty_(n, i, a);
    Wr(this, o, n);
    var l = o.extend_(this, n, i, a);
    return l && Gr(this, o, n), l;
  }, t.defineProperty_ = function(n, i, o) {
    o === void 0 && (o = !1), F(this.keysAtom_);
    try {
      L();
      var a = this.delete_(n);
      if (!a)
        return a;
      if (R(this)) {
        var l = j(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: re,
          newValue: i.value
        });
        if (!l)
          return null;
        var s = l.newValue;
        i.value !== s && (i = ne({}, i, {
          value: s
        }));
      }
      if (o) {
        if (!Reflect.defineProperty(this.target_, n, i))
          return !1;
      } else
        G(this.target_, n, i);
      this.notifyPropertyAddition_(n, i.value);
    } finally {
      I();
    }
    return !0;
  }, t.defineObservableProperty_ = function(n, i, o, a) {
    a === void 0 && (a = !1), F(this.keysAtom_);
    try {
      L();
      var l = this.delete_(n);
      if (!l)
        return l;
      if (R(this)) {
        var s = j(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: re,
          newValue: i
        });
        if (!s)
          return null;
        i = s.newValue;
      }
      var c = Fr(n), u = {
        configurable: h.safeDescriptors ? this.isPlainObject_ : !0,
        enumerable: !0,
        get: c.get,
        set: c.set
      };
      if (a) {
        if (!Reflect.defineProperty(this.target_, n, u))
          return !1;
      } else
        G(this.target_, n, u);
      var d = new de(i, o, process.env.NODE_ENV !== "production" ? this.name_ + "." + n.toString() : "ObservableObject.key", !1);
      this.values_.set(n, d), this.notifyPropertyAddition_(n, d.value_);
    } finally {
      I();
    }
    return !0;
  }, t.defineComputedProperty_ = function(n, i, o) {
    o === void 0 && (o = !1), F(this.keysAtom_);
    try {
      L();
      var a = this.delete_(n);
      if (!a)
        return a;
      if (R(this)) {
        var l = j(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: re,
          newValue: void 0
        });
        if (!l)
          return null;
      }
      i.name || (i.name = process.env.NODE_ENV !== "production" ? this.name_ + "." + n.toString() : "ObservableObject.key"), i.context = this.proxy_ || this.target_;
      var s = Fr(n), c = {
        configurable: h.safeDescriptors ? this.isPlainObject_ : !0,
        enumerable: !1,
        get: s.get,
        set: s.set
      };
      if (o) {
        if (!Reflect.defineProperty(this.target_, n, c))
          return !1;
      } else
        G(this.target_, n, c);
      this.values_.set(n, new _e(i)), this.notifyPropertyAddition_(n, void 0);
    } finally {
      I();
    }
    return !0;
  }, t.delete_ = function(n, i) {
    if (i === void 0 && (i = !1), F(this.keysAtom_), !B(this.target_, n))
      return !0;
    if (R(this)) {
      var o = j(this, {
        object: this.proxy_ || this.target_,
        name: n,
        type: Hr
      });
      if (!o)
        return null;
    }
    try {
      var a, l;
      L();
      var s = z(this), c = process.env.NODE_ENV !== "production" && $(), u = this.values_.get(n), d = void 0;
      if (!u && (s || c)) {
        var f;
        d = (f = St(this.target_, n)) == null ? void 0 : f.value;
      }
      if (i) {
        if (!Reflect.deleteProperty(this.target_, n))
          return !1;
      } else
        delete this.target_[n];
      if (process.env.NODE_ENV !== "production" && delete this.appliedAnnotations_[n], u && (this.values_.delete(n), u instanceof de && (d = u.value_), Hn(u)), this.keysAtom_.reportChanged(), (a = this.pendingKeys_) == null || (l = a.get(n)) == null || l.set(n in this.target_), s || c) {
        var p = {
          type: Hr,
          observableKind: "object",
          object: this.proxy_ || this.target_,
          debugObjectName: this.name_,
          oldValue: d,
          name: n
        };
        process.env.NODE_ENV !== "production" && c && P(p), s && K(this, p), process.env.NODE_ENV !== "production" && c && C();
      }
    } finally {
      I();
    }
    return !0;
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support the fire immediately property for observable objects."), dt(this, n);
  }, t.intercept_ = function(n) {
    return ut(this, n);
  }, t.notifyPropertyAddition_ = function(n, i) {
    var o, a, l = z(this), s = process.env.NODE_ENV !== "production" && $();
    if (l || s) {
      var c = l || s ? {
        type: re,
        observableKind: "object",
        debugObjectName: this.name_,
        object: this.proxy_ || this.target_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && s && P(c), l && K(this, c), process.env.NODE_ENV !== "production" && s && C();
    }
    (o = this.pendingKeys_) == null || (a = o.get(n)) == null || a.set(!0), this.keysAtom_.reportChanged();
  }, t.ownKeys_ = function() {
    return this.keysAtom_.reportObserved(), We(this.target_);
  }, t.keys_ = function() {
    return this.keysAtom_.reportObserved(), Object.keys(this.target_);
  }, e;
}();
function Le(e, t) {
  var r;
  if (process.env.NODE_ENV !== "production" && t && be(e) && v("Options can't be provided for already observable objects."), B(e, _))
    return process.env.NODE_ENV !== "production" && !($r(e) instanceof hr) && v("Cannot convert '" + Rt(e) + `' into observable object:
The target is already observable of different type.
Extending builtins is not supported.`), e;
  process.env.NODE_ENV !== "production" && !Object.isExtensible(e) && v("Cannot make the designated object observable; it is not extensible");
  var n = (r = t?.name) != null ? r : process.env.NODE_ENV !== "production" ? (x(e) ? "ObservableObject" : e.constructor.name) + "@" + U() : "ObservableObject", i = new hr(e, /* @__PURE__ */ new Map(), String(n), Mo(t));
  return Ut(e, _, i), e;
}
var Ca = /* @__PURE__ */ Oe("ObservableObjectAdministration", hr);
function Fr(e) {
  return qr[e] || (qr[e] = {
    get: function() {
      return this[_].getObservablePropValue_(e);
    },
    set: function(r) {
      return this[_].setObservablePropValue_(e, r);
    }
  });
}
function be(e) {
  return Mt(e) ? Ca(e[_]) : !1;
}
function Gr(e, t, r) {
  var n;
  process.env.NODE_ENV !== "production" && (e.appliedAnnotations_[r] = t), (n = e.target_[J]) == null || delete n[r];
}
function Wr(e, t, r) {
  if (process.env.NODE_ENV !== "production" && !fi(t) && v("Cannot annotate '" + e.name_ + "." + r.toString() + "': Invalid annotation."), process.env.NODE_ENV !== "production" && !xt(t) && B(e.appliedAnnotations_, r)) {
    var n = e.name_ + "." + r.toString(), i = e.appliedAnnotations_[r].annotationType_, o = t.annotationType_;
    v("Cannot apply '" + o + "' to '" + n + "':" + (`
The field is already annotated with '` + i + "'.") + `
Re-annotating fields is not allowed.
Use 'override' annotation for methods overridden by subclass.`);
  }
}
var Va = /* @__PURE__ */ hi(0), Ta = /* @__PURE__ */ function() {
  var e = !1, t = {};
  return Object.defineProperty(t, "0", {
    set: function() {
      e = !0;
    }
  }), Object.create(t)[0] = 1, e === !1;
}(), er = 0, di = function() {
};
function Ra(e, t) {
  Object.setPrototypeOf ? Object.setPrototypeOf(e.prototype, t) : e.prototype.__proto__ !== void 0 ? e.prototype.__proto__ = t : e.prototype = t;
}
Ra(di, Array.prototype);
var Sr = /* @__PURE__ */ function(e, t, r) {
  Sn(n, e);
  function n(o, a, l, s) {
    var c;
    return l === void 0 && (l = process.env.NODE_ENV !== "production" ? "ObservableArray@" + U() : "ObservableArray"), s === void 0 && (s = !1), c = e.call(this) || this, we(function() {
      var u = new Nr(l, a, s, !0);
      u.proxy_ = Et(c), An(Et(c), _, u), o && o.length && c.spliceWithArray(0, 0, o), Ta && Object.defineProperty(Et(c), "0", Va);
    }), c;
  }
  var i = n.prototype;
  return i.concat = function() {
    this[_].atom_.reportObserved();
    for (var a = arguments.length, l = new Array(a), s = 0; s < a; s++)
      l[s] = arguments[s];
    return Array.prototype.concat.apply(
      this.slice(),
      //@ts-ignore
      l.map(function(c) {
        return ht(c) ? c.slice() : c;
      })
    );
  }, i[r] = function() {
    var o = this, a = 0;
    return Qe({
      next: function() {
        return a < o.length ? {
          value: o[a++],
          done: !1
        } : {
          done: !0,
          value: void 0
        };
      }
    });
  }, br(n, [{
    key: "length",
    get: function() {
      return this[_].getArrayLength_();
    },
    set: function(a) {
      this[_].setArrayLength_(a);
    }
  }, {
    key: t,
    get: function() {
      return "Array";
    }
  }]), n;
}(di, Symbol.toStringTag, Symbol.iterator);
Object.entries(Vt).forEach(function(e) {
  var t = e[0], r = e[1];
  t !== "concat" && Ut(Sr.prototype, t, r);
});
function hi(e) {
  return {
    enumerable: !1,
    configurable: !0,
    get: function() {
      return this[_].get_(e);
    },
    set: function(r) {
      this[_].set_(e, r);
    }
  };
}
function ja(e) {
  G(Sr.prototype, "" + e, hi(e));
}
function vi(e) {
  if (e > er) {
    for (var t = er; t < e + 100; t++)
      ja(t);
    er = e;
  }
}
vi(1e3);
function La(e, t, r) {
  return new Sr(e, t, r);
}
function ie(e, t) {
  if (typeof e == "object" && e !== null) {
    if (ht(e))
      return t !== void 0 && v(23), e[_].atom_;
    if (je(e))
      return e.atom_;
    if (Q(e)) {
      if (t === void 0)
        return e.keysAtom_;
      var r = e.data_.get(t) || e.hasMap_.get(t);
      return r || v(25, t, Rt(e)), r;
    }
    if (be(e)) {
      if (!t)
        return v(26);
      var n = e[_].values_.get(t);
      return n || v(27, t, Rt(e)), n;
    }
    if (mr(e) || Ft(e) || Ct(e))
      return e;
  } else if (O(e) && Ct(e[_]))
    return e[_];
  v(28);
}
function $r(e, t) {
  if (e || v(29), t !== void 0)
    return $r(ie(e, t));
  if (mr(e) || Ft(e) || Ct(e) || Q(e) || je(e))
    return e;
  if (e[_])
    return e[_];
  v(24, e);
}
function Rt(e, t) {
  var r;
  if (t !== void 0)
    r = ie(e, t);
  else {
    if (ct(e))
      return e.name;
    be(e) || Q(e) || je(e) ? r = $r(e) : r = ie(e);
  }
  return r.name_;
}
function we(e) {
  var t = Ae(), r = qt(!0);
  L();
  try {
    return e();
  } finally {
    I(), Ht(r), Y(t);
  }
}
var Jr = It.toString;
function xr(e, t, r) {
  return r === void 0 && (r = -1), vr(e, t, r);
}
function vr(e, t, r, n, i) {
  if (e === t)
    return e !== 0 || 1 / e === 1 / t;
  if (e == null || t == null)
    return !1;
  if (e !== e)
    return t !== t;
  var o = typeof e;
  if (o !== "function" && o !== "object" && typeof t != "object")
    return !1;
  var a = Jr.call(e);
  if (a !== Jr.call(t))
    return !1;
  switch (a) {
    case "[object RegExp]":
    case "[object String]":
      return "" + e == "" + t;
    case "[object Number]":
      return +e != +e ? +t != +t : +e == 0 ? 1 / +e === 1 / t : +e == +t;
    case "[object Date]":
    case "[object Boolean]":
      return +e == +t;
    case "[object Symbol]":
      return typeof Symbol < "u" && Symbol.valueOf.call(e) === Symbol.valueOf.call(t);
    case "[object Map]":
    case "[object Set]":
      r >= 0 && r++;
      break;
  }
  e = Xr(e), t = Xr(t);
  var l = a === "[object Array]";
  if (!l) {
    if (typeof e != "object" || typeof t != "object")
      return !1;
    var s = e.constructor, c = t.constructor;
    if (s !== c && !(O(s) && s instanceof s && O(c) && c instanceof c) && "constructor" in e && "constructor" in t)
      return !1;
  }
  if (r === 0)
    return !1;
  r < 0 && (r = -1), n = n || [], i = i || [];
  for (var u = n.length; u--; )
    if (n[u] === e)
      return i[u] === t;
  if (n.push(e), i.push(t), l) {
    if (u = e.length, u !== t.length)
      return !1;
    for (; u--; )
      if (!vr(e[u], t[u], r - 1, n, i))
        return !1;
  } else {
    var d = Object.keys(e), f;
    if (u = d.length, Object.keys(t).length !== u)
      return !1;
    for (; u--; )
      if (f = d[u], !(B(t, f) && vr(e[f], t[f], r - 1, n, i)))
        return !1;
  }
  return n.pop(), i.pop(), !0;
}
function Xr(e) {
  return ht(e) ? e.slice() : Te(e) || Q(e) || ot(e) || je(e) ? Array.from(e.entries()) : e;
}
function Qe(e) {
  return e[Symbol.iterator] = Ia, e;
}
function Ia() {
  return this;
}
function fi(e) {
  return (
    // Can be function
    e instanceof Object && typeof e.annotationType_ == "string" && O(e.make_) && O(e.extend_)
  );
}
["Symbol", "Map", "Set"].forEach(function(e) {
  var t = mn();
  typeof t[e] > "u" && v("MobX requires global '" + e + "' to be available or polyfilled");
});
typeof __MOBX_DEVTOOLS_GLOBAL_HOOK__ == "object" && __MOBX_DEVTOOLS_GLOBAL_HOOK__.injectMobx({
  spy: ia,
  extras: {
    getDebugName: Rt
  },
  $mobx: _
});
const Zr = "copilot-conf";
class he {
  static get sessionConfiguration() {
    const t = sessionStorage.getItem(Zr);
    return t ? JSON.parse(t) : {};
  }
  static saveCopilotActivation(t) {
    const r = this.sessionConfiguration;
    r.active = t, this.persist(r);
  }
  static getCopilotActivation() {
    return this.sessionConfiguration.active;
  }
  static saveSpotlightActivation(t) {
    const r = this.sessionConfiguration;
    r.spotlightActive = t, this.persist(r);
  }
  static getSpotlightActivation() {
    return this.sessionConfiguration.spotlightActive;
  }
  static saveSpotlightPosition(t, r, n, i) {
    const o = this.sessionConfiguration;
    o.spotlightPosition = { left: t, top: r, right: n, bottom: i }, this.persist(o);
  }
  static getSpotlightPosition() {
    return this.sessionConfiguration.spotlightPosition;
  }
  static saveDrawerSize(t, r) {
    const n = this.sessionConfiguration;
    n.drawerSizes = n.drawerSizes ?? {}, n.drawerSizes[t] = r, this.persist(n);
  }
  static getDrawerSize(t) {
    const r = this.sessionConfiguration;
    if (r.drawerSizes)
      return r.drawerSizes[t];
  }
  static savePanelConfigurations(t) {
    const r = this.sessionConfiguration;
    r.sectionPanelState = t, this.persist(r);
  }
  static getPanelConfigurations() {
    return this.sessionConfiguration.sectionPanelState;
  }
  static persist(t) {
    sessionStorage.setItem(Zr, JSON.stringify(t));
  }
}
class Ma {
  constructor() {
    this.spotlightActive = !1, this.loginCheckActive = !1, this.userInfo = void 0, this.active = !1, this.activatedAtLeastOnce = !1, this.operationInProgress = void 0, this.operationWaitsHmrUpdate = void 0, this.idePluginState = void 0, this.notifications = [], this.infoTooltip = null, this.feedbackOpened = !1, this.sectionPanelDragging = !1, this.spotlightDragging = !1, this.sectionPanelResizing = !1, wr(this, {
      notifications: w.shallow
    }), this.spotlightActive = he.getSpotlightActivation() ?? !1;
  }
  setActive(t) {
    this.active = t, t && (this.activatedAtLeastOnce = !0);
  }
  setSpotlightActive(t) {
    this.spotlightActive = t;
  }
  setLoginCheckActive(t) {
    this.loginCheckActive = t;
  }
  setUserInfo(t) {
    this.userInfo = t;
  }
  startOperation(t) {
    if (this.operationInProgress)
      throw new Error(`An ${t} operation is already in progress`);
    if (this.operationWaitsHmrUpdate)
      throw new Error("Wait for files to be updated to start a new operation");
    this.operationInProgress = t;
  }
  stopOperation(t) {
    if (this.operationInProgress) {
      if (this.operationInProgress !== t)
        return;
    } else
      return;
    this.operationInProgress = void 0;
  }
  setIdePluginState(t) {
    this.idePluginState = t;
  }
  toggleActive() {
    this.setActive(!this.active);
  }
  reset() {
    this.active = !1, this.activatedAtLeastOnce = !1;
  }
  setNotifications(t) {
    this.notifications = t;
  }
  removeNotification(t) {
    t.animatingOut = !0, setTimeout(() => {
      this.reallyRemoveNotification(t);
    }, 180);
  }
  reallyRemoveNotification(t) {
    const r = this.notifications.indexOf(t);
    r > -1 && this.notifications.splice(r, 1);
  }
  setTooltip(t, r) {
    this.infoTooltip = {
      text: t,
      loader: r
    };
  }
  clearTooltip() {
    this.infoTooltip = null;
  }
  setFeedbackOpened(t) {
    this.feedbackOpened = t;
  }
  setSectionPanelDragging(t) {
    this.sectionPanelDragging = t;
  }
  setSpotlightDragging(t) {
    this.spotlightDragging = t;
  }
  setSectionPanelResizing(t) {
    this.sectionPanelResizing = t;
  }
}
const Yr = "copilot-stored-browser-state";
class Ua {
  constructor() {
    this.activationShortcutEnabled = !0, this.dismissedNotifications = [], wr(this);
    const t = localStorage.getItem(Yr);
    t && Object.assign(this, JSON.parse(t)), Ar(() => {
      localStorage.setItem(Yr, JSON.stringify(this));
    });
  }
  addDismissedNotification(t) {
    this.dismissedNotifications.push(t);
  }
  setActivationShortcutEnabled(t) {
    this.activationShortcutEnabled = t;
  }
  reset() {
    this.activationShortcutEnabled = !0, this.dismissedNotifications = [];
  }
}
const et = "copilot-", ka = "24.4.0.beta3", rl = "attention-required", nl = "https://plugins.jetbrains.com/plugin/23758-vaadin", il = "https://marketplace.visualstudio.com/items?itemName=vaadin.vaadin-vscode", ol = (e, t, r) => t >= e.left && t <= e.right && r >= e.top && r <= e.bottom, Ba = (e) => {
  const t = [];
  let r = Ka(e);
  for (; r; )
    t.push(r), r = r.parentElement;
  return t;
}, za = (e, t) => {
  let r = e;
  for (; !(r instanceof HTMLElement && r.localName === `${et}main`); ) {
    if (!r.isConnected)
      return null;
    if (r.parentNode ? r = r.parentNode : r.host && (r = r.host), r instanceof HTMLElement && r.localName === t)
      return r;
  }
  return null;
};
function Ka(e) {
  return e.parentElement ?? e.parentNode?.host;
}
function tt(e) {
  return !e || !(e instanceof HTMLElement) ? !1 : [...Ba(e), e].map((t) => t.localName).some((t) => t.startsWith(et));
}
function al(e) {
  return e instanceof Element;
}
var pi = /* @__PURE__ */ ((e) => (e["vaadin-combo-box"] = "vaadin-combo-box", e["vaadin-date-picker"] = "vaadin-date-picker", e["vaadin-dialog"] = "vaadin-dialog", e["vaadin-multi-select-combo-box"] = "vaadin-multi-select-combo-box", e["vaadin-select"] = "vaadin-select", e["vaadin-time-picker"] = "vaadin-time-picker", e))(pi || {});
const Ue = {
  "vaadin-combo-box": {
    hideOnActivation: !0,
    open: (e) => _t(e),
    close: (e) => gt(e)
  },
  "vaadin-select": {
    hideOnActivation: !0,
    open: (e) => {
      const t = e;
      gi(t, t._overlayElement), t.opened = !0;
    },
    close: (e) => {
      const t = e;
      bi(t, t._overlayElement), t.opened = !1;
    }
  },
  "vaadin-multi-select-combo-box": {
    hideOnActivation: !0,
    open: (e) => _t(e.$.comboBox),
    close: (e) => {
      gt(e.$.comboBox), e.removeAttribute("focused");
    }
  },
  "vaadin-date-picker": {
    hideOnActivation: !0,
    open: (e) => _t(e),
    close: (e) => gt(e)
  },
  "vaadin-time-picker": {
    hideOnActivation: !0,
    open: (e) => _t(e.$.comboBox),
    close: (e) => {
      gt(e.$.comboBox), e.removeAttribute("focused");
    }
  },
  "vaadin-dialog": {
    hideOnActivation: !1
  }
}, _i = (e) => {
  e.preventDefault(), e.stopImmediatePropagation();
}, _t = (e) => {
  e.addEventListener("focusout", _i, { capture: !0 }), gi(e), e.opened = !0;
}, gt = (e) => {
  bi(e), e.removeAttribute("focused"), e.removeEventListener("focusout", _i, { capture: !0 }), e.opened = !1;
}, gi = (e, t) => {
  const r = t ?? e.$.overlay;
  r.__oldModeless = r.modeless, r.modeless = !0;
}, bi = (e, t) => {
  const r = t ?? e.$.overlay;
  r.modeless = r.__oldModeless !== void 0 ? r.__oldModeless : r.modeless, delete r.__oldModeless;
};
class qa {
  constructor() {
    this.openedOverlayOwners = /* @__PURE__ */ new Set(), this.overlayCloseEventListener = (t) => {
      tt(t.target?.owner) || (window.Vaadin.copilot._uiState.active || tt(t.detail.sourceEvent.target)) && (t.preventDefault(), t.stopImmediatePropagation());
    };
  }
  /**
   * Modifies pointer-events property to auto if dialog overlay is present on body element. <br/>
   * Overriding closeOnOutsideClick method in order to keep overlay present while copilot is active
   * @private
   */
  onCopilotActivation() {
    const t = Array.from(document.body.children).find(
      (i) => i.localName.startsWith("vaadin") && i.localName.endsWith("-overlay")
    );
    if (!t)
      return;
    const r = this.getOwner(t), n = Ue[r.localName];
    n && (n.hideOnActivation && n.close ? n.close(r) : document.body.style.getPropertyValue("pointer-events") === "none" && document.body.style.removeProperty("pointer-events"));
  }
  /**
   * Restores pointer-events state on deactivation. <br/>
   * Closes opened overlays while using copilot.
   * @private
   */
  onCopilotDeactivation() {
    this.openedOverlayOwners.forEach((r) => {
      const n = Ue[r.localName];
      n && n.close && n.close(r);
    }), document.body.querySelector("vaadin-dialog-overlay") && document.body.style.setProperty("pointer-events", "none");
  }
  getOwner(t) {
    const r = t;
    return r.owner ?? r.__dataHost;
  }
  addOverlayOutsideClickEvent() {
    document.documentElement.addEventListener("vaadin-overlay-outside-click", this.overlayCloseEventListener, {
      capture: !0
    }), document.documentElement.addEventListener("vaadin-overlay-escape-press", this.overlayCloseEventListener, {
      capture: !0
    });
  }
  removeOverlayOutsideClickEvent() {
    document.documentElement.removeEventListener("vaadin-overlay-outside-click", this.overlayCloseEventListener), document.documentElement.removeEventListener("vaadin-overlay-escape-press", this.overlayCloseEventListener);
  }
  toggle(t) {
    const r = Ue[t.localName];
    this.isOverlayActive(t) ? (r.close(t), this.openedOverlayOwners.delete(t)) : (r.open(t), this.openedOverlayOwners.add(t));
  }
  isOverlayActive(t) {
    const r = Ue[t.localName];
    return r.active ? r.active(t) : t.hasAttribute("opened");
  }
  overlayStatus(t) {
    if (!t)
      return { visible: !1 };
    const r = t.localName;
    let n = Object.keys(pi).includes(r);
    if (!n)
      return { visible: !1 };
    const i = Ue[t.localName];
    i.hasOverlay && (n = i.hasOverlay(t));
    const o = this.isOverlayActive(t);
    return { visible: n, active: o };
  }
}
window.Vaadin ??= {};
window.Vaadin.copilot ??= {};
window.Vaadin.copilot.plugins = [];
window.Vaadin.copilot._browserState = new Ua();
window.Vaadin.copilot._uiState = new Ma();
window.Vaadin.copilot.eventbus = new qi();
window.Vaadin.copilot.overlayManager = new qa();
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const Ha = (e) => (t, r) => {
  r !== void 0 ? r.addInitializer(() => {
    customElements.define(e, t);
  }) : customElements.define(e, t);
};
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const At = globalThis, Dr = At.ShadowRoot && (At.ShadyCSS === void 0 || At.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype, Pr = Symbol(), Qr = /* @__PURE__ */ new WeakMap();
let mi = class {
  constructor(t, r, n) {
    if (this._$cssResult$ = !0, n !== Pr)
      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t, this.t = r;
  }
  get styleSheet() {
    let t = this.o;
    const r = this.t;
    if (Dr && t === void 0) {
      const n = r !== void 0 && r.length === 1;
      n && (t = Qr.get(r)), t === void 0 && ((this.o = t = new CSSStyleSheet()).replaceSync(this.cssText), n && Qr.set(r, t));
    }
    return t;
  }
  toString() {
    return this.cssText;
  }
};
const $e = (e) => new mi(typeof e == "string" ? e : e + "", void 0, Pr), Fa = (e, ...t) => {
  const r = e.length === 1 ? e[0] : t.reduce((n, i, o) => n + ((a) => {
    if (a._$cssResult$ === !0)
      return a.cssText;
    if (typeof a == "number")
      return a;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + a + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(i) + e[o + 1], e[0]);
  return new mi(r, e, Pr);
}, Ga = (e, t) => {
  if (Dr)
    e.adoptedStyleSheets = t.map((r) => r instanceof CSSStyleSheet ? r : r.styleSheet);
  else
    for (const r of t) {
      const n = document.createElement("style"), i = At.litNonce;
      i !== void 0 && n.setAttribute("nonce", i), n.textContent = r.cssText, e.appendChild(n);
    }
}, en = Dr ? (e) => e : (e) => e instanceof CSSStyleSheet ? ((t) => {
  let r = "";
  for (const n of t.cssRules)
    r += n.cssText;
  return $e(r);
})(e) : e;
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { is: Wa, defineProperty: Ja, getOwnPropertyDescriptor: Xa, getOwnPropertyNames: Za, getOwnPropertySymbols: Ya, getPrototypeOf: Qa } = Object, Gt = globalThis, tn = Gt.trustedTypes, es = tn ? tn.emptyScript : "", ts = Gt.reactiveElementPolyfillSupport, He = (e, t) => e, fr = { toAttribute(e, t) {
  switch (t) {
    case Boolean:
      e = e ? es : null;
      break;
    case Object:
    case Array:
      e = e == null ? e : JSON.stringify(e);
  }
  return e;
}, fromAttribute(e, t) {
  let r = e;
  switch (t) {
    case Boolean:
      r = e !== null;
      break;
    case Number:
      r = e === null ? null : Number(e);
      break;
    case Object:
    case Array:
      try {
        r = JSON.parse(e);
      } catch {
        r = null;
      }
  }
  return r;
} }, yi = (e, t) => !Wa(e, t), rn = { attribute: !0, type: String, converter: fr, reflect: !1, hasChanged: yi };
Symbol.metadata ??= Symbol("metadata"), Gt.litPropertyMetadata ??= /* @__PURE__ */ new WeakMap();
let xe = class extends HTMLElement {
  static addInitializer(t) {
    this._$Ei(), (this.l ??= []).push(t);
  }
  static get observedAttributes() {
    return this.finalize(), this._$Eh && [...this._$Eh.keys()];
  }
  static createProperty(t, r = rn) {
    if (r.state && (r.attribute = !1), this._$Ei(), this.elementProperties.set(t, r), !r.noAccessor) {
      const n = Symbol(), i = this.getPropertyDescriptor(t, n, r);
      i !== void 0 && Ja(this.prototype, t, i);
    }
  }
  static getPropertyDescriptor(t, r, n) {
    const { get: i, set: o } = Xa(this.prototype, t) ?? { get() {
      return this[r];
    }, set(a) {
      this[r] = a;
    } };
    return { get() {
      return i?.call(this);
    }, set(a) {
      const l = i?.call(this);
      o.call(this, a), this.requestUpdate(t, l, n);
    }, configurable: !0, enumerable: !0 };
  }
  static getPropertyOptions(t) {
    return this.elementProperties.get(t) ?? rn;
  }
  static _$Ei() {
    if (this.hasOwnProperty(He("elementProperties")))
      return;
    const t = Qa(this);
    t.finalize(), t.l !== void 0 && (this.l = [...t.l]), this.elementProperties = new Map(t.elementProperties);
  }
  static finalize() {
    if (this.hasOwnProperty(He("finalized")))
      return;
    if (this.finalized = !0, this._$Ei(), this.hasOwnProperty(He("properties"))) {
      const r = this.properties, n = [...Za(r), ...Ya(r)];
      for (const i of n)
        this.createProperty(i, r[i]);
    }
    const t = this[Symbol.metadata];
    if (t !== null) {
      const r = litPropertyMetadata.get(t);
      if (r !== void 0)
        for (const [n, i] of r)
          this.elementProperties.set(n, i);
    }
    this._$Eh = /* @__PURE__ */ new Map();
    for (const [r, n] of this.elementProperties) {
      const i = this._$Eu(r, n);
      i !== void 0 && this._$Eh.set(i, r);
    }
    this.elementStyles = this.finalizeStyles(this.styles);
  }
  static finalizeStyles(t) {
    const r = [];
    if (Array.isArray(t)) {
      const n = new Set(t.flat(1 / 0).reverse());
      for (const i of n)
        r.unshift(en(i));
    } else
      t !== void 0 && r.push(en(t));
    return r;
  }
  static _$Eu(t, r) {
    const n = r.attribute;
    return n === !1 ? void 0 : typeof n == "string" ? n : typeof t == "string" ? t.toLowerCase() : void 0;
  }
  constructor() {
    super(), this._$Ep = void 0, this.isUpdatePending = !1, this.hasUpdated = !1, this._$Em = null, this._$Ev();
  }
  _$Ev() {
    this._$ES = new Promise((t) => this.enableUpdating = t), this._$AL = /* @__PURE__ */ new Map(), this._$E_(), this.requestUpdate(), this.constructor.l?.forEach((t) => t(this));
  }
  addController(t) {
    (this._$EO ??= /* @__PURE__ */ new Set()).add(t), this.renderRoot !== void 0 && this.isConnected && t.hostConnected?.();
  }
  removeController(t) {
    this._$EO?.delete(t);
  }
  _$E_() {
    const t = /* @__PURE__ */ new Map(), r = this.constructor.elementProperties;
    for (const n of r.keys())
      this.hasOwnProperty(n) && (t.set(n, this[n]), delete this[n]);
    t.size > 0 && (this._$Ep = t);
  }
  createRenderRoot() {
    const t = this.shadowRoot ?? this.attachShadow(this.constructor.shadowRootOptions);
    return Ga(t, this.constructor.elementStyles), t;
  }
  connectedCallback() {
    this.renderRoot ??= this.createRenderRoot(), this.enableUpdating(!0), this._$EO?.forEach((t) => t.hostConnected?.());
  }
  enableUpdating(t) {
  }
  disconnectedCallback() {
    this._$EO?.forEach((t) => t.hostDisconnected?.());
  }
  attributeChangedCallback(t, r, n) {
    this._$AK(t, n);
  }
  _$EC(t, r) {
    const n = this.constructor.elementProperties.get(t), i = this.constructor._$Eu(t, n);
    if (i !== void 0 && n.reflect === !0) {
      const o = (n.converter?.toAttribute !== void 0 ? n.converter : fr).toAttribute(r, n.type);
      this._$Em = t, o == null ? this.removeAttribute(i) : this.setAttribute(i, o), this._$Em = null;
    }
  }
  _$AK(t, r) {
    const n = this.constructor, i = n._$Eh.get(t);
    if (i !== void 0 && this._$Em !== i) {
      const o = n.getPropertyOptions(i), a = typeof o.converter == "function" ? { fromAttribute: o.converter } : o.converter?.fromAttribute !== void 0 ? o.converter : fr;
      this._$Em = i, this[i] = a.fromAttribute(r, o.type), this._$Em = null;
    }
  }
  requestUpdate(t, r, n) {
    if (t !== void 0) {
      if (n ??= this.constructor.getPropertyOptions(t), !(n.hasChanged ?? yi)(this[t], r))
        return;
      this.P(t, r, n);
    }
    this.isUpdatePending === !1 && (this._$ES = this._$ET());
  }
  P(t, r, n) {
    this._$AL.has(t) || this._$AL.set(t, r), n.reflect === !0 && this._$Em !== t && (this._$Ej ??= /* @__PURE__ */ new Set()).add(t);
  }
  async _$ET() {
    this.isUpdatePending = !0;
    try {
      await this._$ES;
    } catch (r) {
      Promise.reject(r);
    }
    const t = this.scheduleUpdate();
    return t != null && await t, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    if (!this.isUpdatePending)
      return;
    if (!this.hasUpdated) {
      if (this.renderRoot ??= this.createRenderRoot(), this._$Ep) {
        for (const [i, o] of this._$Ep)
          this[i] = o;
        this._$Ep = void 0;
      }
      const n = this.constructor.elementProperties;
      if (n.size > 0)
        for (const [i, o] of n)
          o.wrapped !== !0 || this._$AL.has(i) || this[i] === void 0 || this.P(i, this[i], o);
    }
    let t = !1;
    const r = this._$AL;
    try {
      t = this.shouldUpdate(r), t ? (this.willUpdate(r), this._$EO?.forEach((n) => n.hostUpdate?.()), this.update(r)) : this._$EU();
    } catch (n) {
      throw t = !1, this._$EU(), n;
    }
    t && this._$AE(r);
  }
  willUpdate(t) {
  }
  _$AE(t) {
    this._$EO?.forEach((r) => r.hostUpdated?.()), this.hasUpdated || (this.hasUpdated = !0, this.firstUpdated(t)), this.updated(t);
  }
  _$EU() {
    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = !1;
  }
  get updateComplete() {
    return this.getUpdateComplete();
  }
  getUpdateComplete() {
    return this._$ES;
  }
  shouldUpdate(t) {
    return !0;
  }
  update(t) {
    this._$Ej &&= this._$Ej.forEach((r) => this._$EC(r, this[r])), this._$EU();
  }
  updated(t) {
  }
  firstUpdated(t) {
  }
};
xe.elementStyles = [], xe.shadowRootOptions = { mode: "open" }, xe[He("elementProperties")] = /* @__PURE__ */ new Map(), xe[He("finalized")] = /* @__PURE__ */ new Map(), ts?.({ ReactiveElement: xe }), (Gt.reactiveElementVersions ??= []).push("2.0.4");
const Se = Symbol("LitMobxRenderReaction"), nn = Symbol("LitMobxRequestUpdate");
function rs(e, t) {
  var r, n;
  return n = class extends e {
    constructor() {
      super(...arguments), this[r] = () => {
        this.requestUpdate();
      };
    }
    connectedCallback() {
      super.connectedCallback();
      const o = this.constructor.name || this.nodeName;
      this[Se] = new t(`${o}.update()`, this[nn]), this.hasUpdated && this.requestUpdate();
    }
    disconnectedCallback() {
      super.disconnectedCallback(), this[Se] && (this[Se].dispose(), this[Se] = void 0);
    }
    update(o) {
      this[Se] ? this[Se].track(super.update.bind(this, o)) : super.update(o);
    }
  }, r = nn, n;
}
function ns(e) {
  return rs(e, Je);
}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const Cr = globalThis, jt = Cr.trustedTypes, on = jt ? jt.createPolicy("lit-html", { createHTML: (e) => e }) : void 0, Vr = "$lit$", Z = `lit$${(Math.random() + "").slice(9)}$`, Tr = "?" + Z, is = `<${Tr}>`, me = document, rt = () => me.createComment(""), nt = (e) => e === null || typeof e != "object" && typeof e != "function", Ei = Array.isArray, Oi = (e) => Ei(e) || typeof e?.[Symbol.iterator] == "function", tr = `[ 	
\f\r]`, ke = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g, an = /-->/g, sn = />/g, se = RegExp(`>|${tr}(?:([^\\s"'>=/]+)(${tr}*=${tr}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g"), ln = /'/g, cn = /"/g, Ai = /^(?:script|style|textarea|title)$/i, wi = (e) => (t, ...r) => ({ _$litType$: e, strings: t, values: r }), pr = wi(1), ul = wi(2), ye = Symbol.for("lit-noChange"), A = Symbol.for("lit-nothing"), un = /* @__PURE__ */ new WeakMap(), ue = me.createTreeWalker(me, 129);
function Ni(e, t) {
  if (!Array.isArray(e) || !e.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return on !== void 0 ? on.createHTML(t) : t;
}
const Si = (e, t) => {
  const r = e.length - 1, n = [];
  let i, o = t === 2 ? "<svg>" : "", a = ke;
  for (let l = 0; l < r; l++) {
    const s = e[l];
    let c, u, d = -1, f = 0;
    for (; f < s.length && (a.lastIndex = f, u = a.exec(s), u !== null); )
      f = a.lastIndex, a === ke ? u[1] === "!--" ? a = an : u[1] !== void 0 ? a = sn : u[2] !== void 0 ? (Ai.test(u[2]) && (i = RegExp("</" + u[2], "g")), a = se) : u[3] !== void 0 && (a = se) : a === se ? u[0] === ">" ? (a = i ?? ke, d = -1) : u[1] === void 0 ? d = -2 : (d = a.lastIndex - u[2].length, c = u[1], a = u[3] === void 0 ? se : u[3] === '"' ? cn : ln) : a === cn || a === ln ? a = se : a === an || a === sn ? a = ke : (a = se, i = void 0);
    const p = a === se && e[l + 1].startsWith("/>") ? " " : "";
    o += a === ke ? s + is : d >= 0 ? (n.push(c), s.slice(0, d) + Vr + s.slice(d) + Z + p) : s + Z + (d === -2 ? l : p);
  }
  return [Ni(e, o + (e[r] || "<?>") + (t === 2 ? "</svg>" : "")), n];
};
class it {
  constructor({ strings: t, _$litType$: r }, n) {
    let i;
    this.parts = [];
    let o = 0, a = 0;
    const l = t.length - 1, s = this.parts, [c, u] = Si(t, r);
    if (this.el = it.createElement(c, n), ue.currentNode = this.el.content, r === 2) {
      const d = this.el.content.firstChild;
      d.replaceWith(...d.childNodes);
    }
    for (; (i = ue.nextNode()) !== null && s.length < l; ) {
      if (i.nodeType === 1) {
        if (i.hasAttributes())
          for (const d of i.getAttributeNames())
            if (d.endsWith(Vr)) {
              const f = u[a++], p = i.getAttribute(d).split(Z), y = /([.?@])?(.*)/.exec(f);
              s.push({ type: 1, index: o, name: y[2], strings: p, ctor: y[1] === "." ? xi : y[1] === "?" ? Di : y[1] === "@" ? Pi : vt }), i.removeAttribute(d);
            } else
              d.startsWith(Z) && (s.push({ type: 6, index: o }), i.removeAttribute(d));
        if (Ai.test(i.tagName)) {
          const d = i.textContent.split(Z), f = d.length - 1;
          if (f > 0) {
            i.textContent = jt ? jt.emptyScript : "";
            for (let p = 0; p < f; p++)
              i.append(d[p], rt()), ue.nextNode(), s.push({ type: 2, index: ++o });
            i.append(d[f], rt());
          }
        }
      } else if (i.nodeType === 8)
        if (i.data === Tr)
          s.push({ type: 2, index: o });
        else {
          let d = -1;
          for (; (d = i.data.indexOf(Z, d + 1)) !== -1; )
            s.push({ type: 7, index: o }), d += Z.length - 1;
        }
      o++;
    }
  }
  static createElement(t, r) {
    const n = me.createElement("template");
    return n.innerHTML = t, n;
  }
}
function Ee(e, t, r = e, n) {
  if (t === ye)
    return t;
  let i = n !== void 0 ? r._$Co?.[n] : r._$Cl;
  const o = nt(t) ? void 0 : t._$litDirective$;
  return i?.constructor !== o && (i?._$AO?.(!1), o === void 0 ? i = void 0 : (i = new o(e), i._$AT(e, r, n)), n !== void 0 ? (r._$Co ??= [])[n] = i : r._$Cl = i), i !== void 0 && (t = Ee(e, i._$AS(e, t.values), i, n)), t;
}
class $i {
  constructor(t, r) {
    this._$AV = [], this._$AN = void 0, this._$AD = t, this._$AM = r;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t) {
    const { el: { content: r }, parts: n } = this._$AD, i = (t?.creationScope ?? me).importNode(r, !0);
    ue.currentNode = i;
    let o = ue.nextNode(), a = 0, l = 0, s = n[0];
    for (; s !== void 0; ) {
      if (a === s.index) {
        let c;
        s.type === 2 ? c = new Ie(o, o.nextSibling, this, t) : s.type === 1 ? c = new s.ctor(o, s.name, s.strings, this, t) : s.type === 6 && (c = new Ci(o, this, t)), this._$AV.push(c), s = n[++l];
      }
      a !== s?.index && (o = ue.nextNode(), a++);
    }
    return ue.currentNode = me, i;
  }
  p(t) {
    let r = 0;
    for (const n of this._$AV)
      n !== void 0 && (n.strings !== void 0 ? (n._$AI(t, n, r), r += n.strings.length - 2) : n._$AI(t[r])), r++;
  }
}
class Ie {
  get _$AU() {
    return this._$AM?._$AU ?? this._$Cv;
  }
  constructor(t, r, n, i) {
    this.type = 2, this._$AH = A, this._$AN = void 0, this._$AA = t, this._$AB = r, this._$AM = n, this.options = i, this._$Cv = i?.isConnected ?? !0;
  }
  get parentNode() {
    let t = this._$AA.parentNode;
    const r = this._$AM;
    return r !== void 0 && t?.nodeType === 11 && (t = r.parentNode), t;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t, r = this) {
    t = Ee(this, t, r), nt(t) ? t === A || t == null || t === "" ? (this._$AH !== A && this._$AR(), this._$AH = A) : t !== this._$AH && t !== ye && this._(t) : t._$litType$ !== void 0 ? this.$(t) : t.nodeType !== void 0 ? this.T(t) : Oi(t) ? this.k(t) : this._(t);
  }
  S(t) {
    return this._$AA.parentNode.insertBefore(t, this._$AB);
  }
  T(t) {
    this._$AH !== t && (this._$AR(), this._$AH = this.S(t));
  }
  _(t) {
    this._$AH !== A && nt(this._$AH) ? this._$AA.nextSibling.data = t : this.T(me.createTextNode(t)), this._$AH = t;
  }
  $(t) {
    const { values: r, _$litType$: n } = t, i = typeof n == "number" ? this._$AC(t) : (n.el === void 0 && (n.el = it.createElement(Ni(n.h, n.h[0]), this.options)), n);
    if (this._$AH?._$AD === i)
      this._$AH.p(r);
    else {
      const o = new $i(i, this), a = o.u(this.options);
      o.p(r), this.T(a), this._$AH = o;
    }
  }
  _$AC(t) {
    let r = un.get(t.strings);
    return r === void 0 && un.set(t.strings, r = new it(t)), r;
  }
  k(t) {
    Ei(this._$AH) || (this._$AH = [], this._$AR());
    const r = this._$AH;
    let n, i = 0;
    for (const o of t)
      i === r.length ? r.push(n = new Ie(this.S(rt()), this.S(rt()), this, this.options)) : n = r[i], n._$AI(o), i++;
    i < r.length && (this._$AR(n && n._$AB.nextSibling, i), r.length = i);
  }
  _$AR(t = this._$AA.nextSibling, r) {
    for (this._$AP?.(!1, !0, r); t && t !== this._$AB; ) {
      const n = t.nextSibling;
      t.remove(), t = n;
    }
  }
  setConnected(t) {
    this._$AM === void 0 && (this._$Cv = t, this._$AP?.(t));
  }
}
class vt {
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  constructor(t, r, n, i, o) {
    this.type = 1, this._$AH = A, this._$AN = void 0, this.element = t, this.name = r, this._$AM = i, this.options = o, n.length > 2 || n[0] !== "" || n[1] !== "" ? (this._$AH = Array(n.length - 1).fill(new String()), this.strings = n) : this._$AH = A;
  }
  _$AI(t, r = this, n, i) {
    const o = this.strings;
    let a = !1;
    if (o === void 0)
      t = Ee(this, t, r, 0), a = !nt(t) || t !== this._$AH && t !== ye, a && (this._$AH = t);
    else {
      const l = t;
      let s, c;
      for (t = o[0], s = 0; s < o.length - 1; s++)
        c = Ee(this, l[n + s], r, s), c === ye && (c = this._$AH[s]), a ||= !nt(c) || c !== this._$AH[s], c === A ? t = A : t !== A && (t += (c ?? "") + o[s + 1]), this._$AH[s] = c;
    }
    a && !i && this.j(t);
  }
  j(t) {
    t === A ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t ?? "");
  }
}
class xi extends vt {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t) {
    this.element[this.name] = t === A ? void 0 : t;
  }
}
class Di extends vt {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t) {
    this.element.toggleAttribute(this.name, !!t && t !== A);
  }
}
class Pi extends vt {
  constructor(t, r, n, i, o) {
    super(t, r, n, i, o), this.type = 5;
  }
  _$AI(t, r = this) {
    if ((t = Ee(this, t, r, 0) ?? A) === ye)
      return;
    const n = this._$AH, i = t === A && n !== A || t.capture !== n.capture || t.once !== n.once || t.passive !== n.passive, o = t !== A && (n === A || i);
    i && this.element.removeEventListener(this.name, this, n), o && this.element.addEventListener(this.name, this, t), this._$AH = t;
  }
  handleEvent(t) {
    typeof this._$AH == "function" ? this._$AH.call(this.options?.host ?? this.element, t) : this._$AH.handleEvent(t);
  }
}
class Ci {
  constructor(t, r, n) {
    this.element = t, this.type = 6, this._$AN = void 0, this._$AM = r, this.options = n;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t) {
    Ee(this, t);
  }
}
const os = { P: Vr, A: Z, C: Tr, M: 1, L: Si, R: $i, D: Oi, V: Ee, I: Ie, H: vt, N: Di, U: Pi, B: xi, F: Ci }, as = Cr.litHtmlPolyfillSupport;
as?.(it, Ie), (Cr.litHtmlVersions ??= []).push("3.1.2");
const ss = (e, t, r) => {
  const n = r?.renderBefore ?? t;
  let i = n._$litPart$;
  if (i === void 0) {
    const o = r?.renderBefore ?? null;
    n._$litPart$ = i = new Ie(t.insertBefore(rt(), o), o, void 0, r ?? {});
  }
  return i._$AI(e), i;
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
let Fe = class extends xe {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    const t = super.createRenderRoot();
    return this.renderOptions.renderBefore ??= t.firstChild, t;
  }
  update(t) {
    const r = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(t), this._$Do = ss(r, this.renderRoot, this.renderOptions);
  }
  connectedCallback() {
    super.connectedCallback(), this._$Do?.setConnected(!0);
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this._$Do?.setConnected(!1);
  }
  render() {
    return ye;
  }
};
Fe._$litElement$ = !0, Fe.finalized = !0, globalThis.litElementHydrateSupport?.({ LitElement: Fe });
const ls = globalThis.litElementPolyfillSupport;
ls?.({ LitElement: Fe });
(globalThis.litElementVersions ??= []).push("4.0.4");
class cs extends ns(Fe) {
}
class us extends cs {
  constructor() {
    super(...arguments), this.disposers = [];
  }
  /**
   * Creates a MobX reaction using the given parameters and disposes it when this element is detached.
   *
   * This should be called from `connectedCallback` to ensure that the reaction is active also if the element is attached again later.
   */
  reaction(t, r, n) {
    this.disposers.push(ua(t, r, n));
  }
  /**
   * Creates a MobX autorun using the given parameters and disposes it when this element is detached.
   *
   * This should be called from `connectedCallback` to ensure that the reaction is active also if the element is attached again later.
   */
  autorun(t, r) {
    this.disposers.push(Ar(t, r));
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this.disposers.forEach((t) => {
      t();
    }), this.disposers = [];
  }
}
class ds {
  constructor() {
    this._panels = [], this._attentionRequiredPanelTag = null, this._floatingPanelsZIndexOrder = [], wr(this), this.restorePositions();
  }
  restorePositions() {
    const t = he.getPanelConfigurations();
    t && (this._panels = this._panels.map((r) => {
      const n = t.find((i) => i.tag === r.tag);
      return n && (r = Object.assign(r, { ...n })), r;
    }));
  }
  /**
   * Adds panelTag as last element -focused- to list.
   * @param panelConfiguration
   */
  addFocusedFloatingPanel(t) {
    this._floatingPanelsZIndexOrder = this._floatingPanelsZIndexOrder.filter((r) => r !== t.tag), t.floating && this._floatingPanelsZIndexOrder.push(t.tag);
  }
  /**
   * Returns the focused z-index of floating panel as following order
   * <ul>
   *     <li>Returns 50 for last(focused) element </li>
   *     <li>Returns the index of element in list(starting from 0) </li>
   *     <li>Returns 0 if panel is not in the list</li>
   * </ul>
   * @param panelTag
   */
  getFloatingPanelZIndex(t) {
    const r = this._floatingPanelsZIndexOrder.findIndex((n) => n === t);
    return r === this._floatingPanelsZIndexOrder.length - 1 ? 50 : r === -1 ? 0 : r;
  }
  get floatingPanelsZIndexOrder() {
    return this._floatingPanelsZIndexOrder;
  }
  get attentionRequiredPanelTag() {
    return this._attentionRequiredPanelTag;
  }
  set attentionRequiredPanelTag(t) {
    this._attentionRequiredPanelTag = t;
  }
  getAttentionRequiredPanelConfiguration() {
    return this._panels.find((t) => t.tag === this._attentionRequiredPanelTag);
  }
  clearAttention() {
    this._attentionRequiredPanelTag = null;
  }
  get panels() {
    return this._panels;
  }
  addPanel(t) {
    this._panels.push(t), this.restorePositions();
  }
  getPanelByTag(t) {
    return this._panels.find((r) => r.tag === t);
  }
  updatePanel(t, r) {
    const n = [...this._panels], i = n.find((o) => o.tag === t);
    if (i) {
      for (const o in r)
        i[o] = r[o];
      r.floating === !1 && (this._floatingPanelsZIndexOrder = this._floatingPanelsZIndexOrder.filter((o) => o !== t)), this._panels = n, he.savePanelConfigurations(this._panels);
    }
  }
  updateOrders(t) {
    const r = [...this._panels];
    r.forEach((n) => {
      const i = t.find((o) => o.tag === n.tag);
      i && (n.panelOrder = i.order);
    }), this._panels = r, he.savePanelConfigurations(r);
  }
}
const Wt = new ds();
function Vi(e, t) {
  const r = e();
  r ? t(r) : setTimeout(() => Vi(e, t), 50);
}
async function Ti(e) {
  let t;
  const r = new Promise((i) => {
    t = i;
  }), n = setInterval(() => {
    const i = e();
    i && (clearInterval(n), t(i));
  }, 10);
  return r;
}
function hs(e) {
  return e && typeof e.lastAccessedBy_ == "number";
}
function hl(e) {
  if (e) {
    if (typeof e == "string")
      return e;
    if (!hs(e))
      throw new Error(`Expected message to be a string or an observable value but was ${JSON.stringify(e)}`);
    return e.get();
  }
}
function vl(e, t) {
  return e.length > t ? `${e.substring(0, t - 3)}...` : e;
}
const vs = {
  userAgent: navigator.userAgent,
  locale: navigator.language,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
};
async function Rr() {
  return Ti(() => {
    const e = window.Vaadin.devTools, t = e?.frontendConnection && e?.frontendConnection.status === "active";
    return e !== void 0 && t && e?.frontendConnection;
  });
}
function Jt(e, t) {
  Rr().then((r) => r.send(e, { ...t, context: vs }));
}
async function fl() {
  return await Rr(), !!window.Vaadin.devTools.conf.backend;
}
let Ge = [];
const dn = [];
function hn(e) {
  e.init({
    addPanel: (t) => {
      Wt.addPanel(t);
    },
    send(t, r) {
      Jt(t, r);
    }
  });
}
function fs() {
  Ge.push(import("./copilot-log-plugin-BG6nz1SN.js")), Ge.push(import("./copilot-info-plugin-BA-xhjEc.js")), Ge.push(import("./copilot-features-plugin-Dqf8mDTA.js"));
}
function ps() {
  {
    const e = `https://cdn.vaadin.com/copilot/${ka}/copilot-plugins.js`;
    import(
      /* @vite-ignore */
      e
    ).catch((t) => {
      console.warn(`Unable to load plugins from ${e}. Some Copilot features are unavailable.`, t);
    });
  }
}
function _s() {
  Promise.all(Ge).then(() => {
    const e = window.Vaadin;
    if (e.copilot.plugins) {
      const t = e.copilot.plugins;
      e.copilot.plugins.push = (r) => hn(r), Array.from(t).forEach((r) => {
        dn.includes(r) || (hn(r), dn.push(r));
      });
    }
  }), Ge = [];
}
class gs {
  constructor() {
    this.active = !1, this.activate = () => {
      this.active = !0, this.blurActiveApplicationElement();
    }, this.deactivate = () => {
      this.active = !1;
    }, this.focusInEventListener = (t) => {
      this.active && (tt(t.target) || (t.target.blur && t.target.blur(), document.body.querySelector("copilot-main")?.focus()));
    };
  }
  hostConnectedCallback() {
    const t = this.getApplicationRootElement();
    t && t instanceof HTMLElement && t.addEventListener("focusin", this.focusInEventListener);
  }
  hostDisconnectedCallback() {
    const t = this.getApplicationRootElement();
    t && t instanceof HTMLElement && t.removeEventListener("focusin", this.focusInEventListener);
  }
  getApplicationRootElement() {
    return document.body.firstElementChild;
  }
  blurActiveApplicationElement() {
    document.activeElement && document.activeElement.blur && document.activeElement.blur();
  }
}
const bt = new gs(), T = window.Vaadin.copilot.eventbus;
if (!T)
  throw new Error("Tried to access copilot eventbus before it was initialized.");
const Be = window.Vaadin.copilot.overlayManager, pl = {
  AddClickListener: "Add Click Listener",
  AI: "AI",
  Delete: "Delete",
  DragAndDrop: "Drag and Drop",
  Duplicate: "Duplicate",
  SetLabel: "Set label",
  SetText: "Set text",
  SetHelper: "Set helper text",
  WrapWithTag: "Wrapping with tag",
  Alignment: "Alignment",
  ModifyComponentSource: "Modify component source"
}, g = window.Vaadin.copilot._uiState;
if (!g)
  throw new Error("Tried to access copilot ui state before it was initialized.");
const Ri = (e, t) => {
  Jt("copilot-track-event", { event: e, value: t });
}, bs = async (e, t, r) => window.Vaadin.copilot.comm(e, t, r);
var ji = /* @__PURE__ */ ((e) => (e.INFORMATION = "information", e.WARNING = "warning", e.ERROR = "error", e))(ji || {});
function ms() {
  return import("./copilot-notification-jGQjkdF7.js").then((e) => e.c);
}
function Li(e, t) {
  ms().then(({ showNotification: r }) => {
    r({
      type: ji.ERROR,
      message: "Copilot internal error",
      details: e + (t ? `
${t}` : "")
    });
  }), Ri("error", `${e}
\`\`\`${t}\`\`\``);
}
const Ii = () => {
  ys().then((e) => g.setUserInfo(e)).catch((e) => Li("Failed to load userInfo", e));
}, ys = async () => bs(`${et}get-user-info`, {}, (e) => (delete e.data.reqId, e.data)), Es = async () => Ti(() => g.userInfo), _l = async () => (await Es()).accessTo.includes("vaadin-employee"), Mi = window.Vaadin.copilot._browserState;
if (!Mi)
  throw new Error("Tried to access copilot browser state before it was initialized.");
function Os(e) {
  return e.composed && e.composedPath().map((t) => t.localName).some((t) => t === "copilot-spotlight");
}
function As(e) {
  return e.composed && e.composedPath().map((t) => t.localName).some((t) => t === "copilot-drawer-panel" || t === "copilot-section-panel-wrapper");
}
let rr = !1, mt = 0;
const vn = (e) => {
  if (Mi.activationShortcutEnabled)
    if (e.key === "Shift" && !e.ctrlKey && !e.altKey && !e.metaKey)
      rr = !0;
    else if (rr && e.shiftKey && (e.key === "Control" || e.key === "Meta")) {
      if (mt++, mt === 2) {
        g.toggleActive();
        return;
      }
      setTimeout(() => {
        mt = 0;
      }, 500);
    } else
      rr = !1, mt = 0;
  g.active && ws(e);
}, ws = (e) => {
  const t = Os(e);
  if (e.shiftKey && e.code === "Space") {
    if (g.feedbackOpened)
      return;
    g.setSpotlightActive(!g.spotlightActive), e.stopPropagation(), e.preventDefault();
  } else if (e.key === "Escape") {
    if (e.stopPropagation(), g.loginCheckActive) {
      g.setLoginCheckActive(!1);
      return;
    }
    T.emit("close-drawers", {}), g.setSpotlightActive(!1);
  } else
    !As(e) && !t && Ns(e) ? (T.emit("delete-selected", {}), e.preventDefault(), e.stopPropagation()) : (e.ctrlKey || e.metaKey) && e.key === "d" && !t ? (T.emit("duplicate-selected", {}), e.preventDefault(), e.stopPropagation()) : (e.ctrlKey || e.metaKey) && e.key === "z" && g.idePluginState?.supportedActions?.find((r) => r === "undo") && (T.emit("undoRedo", { undo: !e.shiftKey }), e.preventDefault(), e.stopPropagation());
}, Ns = (e) => (e.key === "Backspace" || e.key === "Delete") && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey;
/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const Ui = Symbol.for(""), Ss = (e) => {
  if (e?.r === Ui)
    return e?._$litStatic$;
}, ki = (e) => ({ _$litStatic$: e, r: Ui }), fn = /* @__PURE__ */ new Map(), $s = (e) => (t, ...r) => {
  const n = r.length;
  let i, o;
  const a = [], l = [];
  let s, c = 0, u = !1;
  for (; c < n; ) {
    for (s = t[c]; c < n && (o = r[c], (i = Ss(o)) !== void 0); )
      s += i + t[++c], u = !0;
    c !== n && l.push(o), a.push(s), c++;
  }
  if (c === n && a.push(t[n]), u) {
    const d = a.join("$$lit$$");
    (t = fn.get(d)) === void 0 && (a.raw = a, fn.set(d, t = a)), r = l;
  }
  return e(t, ...r);
}, Lt = $s(pr);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const xs = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 }, Ds = (e) => (...t) => ({ _$litDirective$: e, values: t });
class Ps {
  constructor(t) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t, r, n) {
    this._$Ct = t, this._$AM = r, this._$Ci = n;
  }
  _$AS(t, r) {
    return this.update(t, r);
  }
  update(t, r) {
    return this.render(...r);
  }
}
/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { I: Cs } = os, gl = (e) => e.strings === void 0, pn = () => document.createComment(""), ze = (e, t, r) => {
  const n = e._$AA.parentNode, i = t === void 0 ? e._$AB : t._$AA;
  if (r === void 0) {
    const o = n.insertBefore(pn(), i), a = n.insertBefore(pn(), i);
    r = new Cs(o, a, e, e.options);
  } else {
    const o = r._$AB.nextSibling, a = r._$AM, l = a !== e;
    if (l) {
      let s;
      r._$AQ?.(e), r._$AM = e, r._$AP !== void 0 && (s = e._$AU) !== a._$AU && r._$AP(s);
    }
    if (o !== i || l) {
      let s = r._$AA;
      for (; s !== o; ) {
        const c = s.nextSibling;
        n.insertBefore(s, i), s = c;
      }
    }
  }
  return r;
}, le = (e, t, r = e) => (e._$AI(t, r), e), Vs = {}, Ts = (e, t = Vs) => e._$AH = t, Rs = (e) => e._$AH, nr = (e) => {
  e._$AP?.(!1, !0);
  let t = e._$AA;
  const r = e._$AB.nextSibling;
  for (; t !== r; ) {
    const n = t.nextSibling;
    t.remove(), t = n;
  }
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const _n = (e, t, r) => {
  const n = /* @__PURE__ */ new Map();
  for (let i = t; i <= r; i++)
    n.set(e[i], i);
  return n;
}, Bi = Ds(class extends Ps {
  constructor(e) {
    if (super(e), e.type !== xs.CHILD)
      throw Error("repeat() can only be used in text expressions");
  }
  dt(e, t, r) {
    let n;
    r === void 0 ? r = t : t !== void 0 && (n = t);
    const i = [], o = [];
    let a = 0;
    for (const l of e)
      i[a] = n ? n(l, a) : a, o[a] = r(l, a), a++;
    return { values: o, keys: i };
  }
  render(e, t, r) {
    return this.dt(e, t, r).values;
  }
  update(e, [t, r, n]) {
    const i = Rs(e), { values: o, keys: a } = this.dt(t, r, n);
    if (!Array.isArray(i))
      return this.ut = a, o;
    const l = this.ut ??= [], s = [];
    let c, u, d = 0, f = i.length - 1, p = 0, y = o.length - 1;
    for (; d <= f && p <= y; )
      if (i[d] === null)
        d++;
      else if (i[f] === null)
        f--;
      else if (l[d] === a[p])
        s[p] = le(i[d], o[p]), d++, p++;
      else if (l[f] === a[y])
        s[y] = le(i[f], o[y]), f--, y--;
      else if (l[d] === a[y])
        s[y] = le(i[d], o[y]), ze(e, s[y + 1], i[d]), d++, y--;
      else if (l[f] === a[p])
        s[p] = le(i[f], o[p]), ze(e, i[d], i[f]), f--, p++;
      else if (c === void 0 && (c = _n(a, p, y), u = _n(l, d, f)), c.has(l[d]))
        if (c.has(l[f])) {
          const m = u.get(a[p]), N = m !== void 0 ? i[m] : null;
          if (N === null) {
            const H = ze(e, i[d]);
            le(H, o[p]), s[p] = H;
          } else
            s[p] = le(N, o[p]), ze(e, i[d], N), i[m] = null;
          p++;
        } else
          nr(i[f]), f--;
      else
        nr(i[d]), d++;
    for (; p <= y; ) {
      const m = ze(e, s[y + 1]);
      le(m, o[p]), s[p++] = m;
    }
    for (; d <= f; ) {
      const m = i[d++];
      m !== null && nr(m);
    }
    return this.ut = a, Ts(e, s), ye;
  }
}), wt = /* @__PURE__ */ new Map(), js = (e) => {
  const r = Wt.panels.filter((n) => !n.floating && n.panel === e).sort((n, i) => n.panelOrder - i.panelOrder);
  return Lt`
    ${Bi(
    r,
    (n) => n.tag,
    (n) => {
      const i = ki(n.tag);
      return Lt`
                        <copilot-section-panel-wrapper panelTag="${i}">
                            <${i} slot="content"></${i}>
                        </copilot-section-panel-wrapper>`;
    }
  )}
  `;
}, Ls = () => {
  const e = Wt.panels;
  return Lt`
    ${Bi(
    e.filter((t) => t.floating),
    (t) => t.tag,
    (t) => {
      const r = ki(t.tag);
      return Lt`
                        <copilot-section-panel-wrapper panelTag="${r}">
                            <${r} slot="content"></${r}>
                        </copilot-section-panel-wrapper>`;
    }
  )}
  `;
}, bl = (e) => {
  const t = e.panelTag, r = e.querySelector('[slot="content"]');
  r && wt.set(t, r);
}, ml = (e) => {
  if (wt.has(e.panelTag)) {
    const t = wt.get(e.panelTag);
    e.querySelector('[slot="content"]').replaceWith(t);
  }
  wt.delete(e.panelTag);
};
let yt;
const Is = new Uint8Array(16);
function Ms() {
  if (!yt && (yt = typeof crypto < "u" && crypto.getRandomValues && crypto.getRandomValues.bind(crypto), !yt))
    throw new Error("crypto.getRandomValues() not supported. See https://github.com/uuidjs/uuid#getrandomvalues-not-supported");
  return yt(Is);
}
const S = [];
for (let e = 0; e < 256; ++e)
  S.push((e + 256).toString(16).slice(1));
function Us(e, t = 0) {
  return S[e[t + 0]] + S[e[t + 1]] + S[e[t + 2]] + S[e[t + 3]] + "-" + S[e[t + 4]] + S[e[t + 5]] + "-" + S[e[t + 6]] + S[e[t + 7]] + "-" + S[e[t + 8]] + S[e[t + 9]] + "-" + S[e[t + 10]] + S[e[t + 11]] + S[e[t + 12]] + S[e[t + 13]] + S[e[t + 14]] + S[e[t + 15]];
}
const ks = typeof crypto < "u" && crypto.randomUUID && crypto.randomUUID.bind(crypto), gn = {
  randomUUID: ks
};
function Bs(e, t, r) {
  if (gn.randomUUID && !t && !e)
    return gn.randomUUID();
  e = e || {};
  const n = e.random || (e.rng || Ms)();
  if (n[6] = n[6] & 15 | 64, n[8] = n[8] & 63 | 128, t) {
    r = r || 0;
    for (let i = 0; i < 16; ++i)
      t[r + i] = n[i];
    return t;
  }
  return Us(n);
}
const Nt = [], Ke = [], yl = async (e, t, r) => {
  let n, i;
  t.reqId = Bs();
  const o = new Promise((a, l) => {
    n = a, i = l;
  });
  return Nt.push({
    handleMessage(a) {
      if (a?.data?.reqId !== t.reqId)
        return !1;
      try {
        n(r(a));
      } catch (l) {
        i(l.toString());
      }
      return !0;
    }
  }), Jt(e, t), o;
};
function zs(e) {
  for (const t of Nt)
    if (t.handleMessage(e))
      return Nt.splice(Nt.indexOf(t), 1), !0;
  if (e.command === "copilot-plugin-state")
    return g.setIdePluginState(e.data), !0;
  if (e.command === "copilot-prokey-received")
    return Ii(), !0;
  T.emitUnsafe({ type: e.command, data: e.data });
  for (const t of Ki())
    if (zi(t, e))
      return !0;
  return Ke.push(e), !1;
}
function zi(e, t) {
  return e.handleMessage?.call(e, t);
}
function Ks() {
  if (Ke.length)
    for (const e of Ki())
      for (let t = 0; t < Ke.length; t++)
        zi(e, Ke[t]) && (Ke.splice(t, 1), t--);
}
function Ki() {
  const e = document.querySelector("copilot-main");
  return e ? e.renderRoot.querySelectorAll("copilot-section-panel-wrapper *") : [];
}
const qs = ":host{--gray-h: 220;--gray-s: 30%;--gray-l: 30%;--gray-hsl: var(--gray-h) var(--gray-s) var(--gray-l);--gray: hsl(var(--gray-hsl));--gray-50: hsl(var(--gray-hsl) / .05);--gray-100: hsl(var(--gray-hsl) / .1);--gray-150: hsl(var(--gray-hsl) / .16);--gray-200: hsl(var(--gray-hsl) / .24);--gray-250: hsl(var(--gray-hsl) / .34);--gray-300: hsl(var(--gray-hsl) / .46);--gray-350: hsl(var(--gray-hsl) / .6);--gray-400: hsl(var(--gray-hsl) / .7);--gray-450: hsl(var(--gray-hsl) / .8);--gray-500: hsl(var(--gray-hsl) / .9);--gray-550: hsl(var(--gray-hsl));--gray-600: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 2%));--gray-650: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 4%));--gray-700: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 8%));--gray-750: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 12%));--gray-800: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 20%));--gray-850: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 23%));--gray-900: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 30%));--blue-h: 220;--blue-s: 90%;--blue-l: 53%;--blue-hsl: var(--blue-h) var(--blue-s) var(--blue-l);--blue: hsl(var(--blue-hsl));--blue-50: hsl(var(--blue-hsl) / .05);--blue-100: hsl(var(--blue-hsl) / .1);--blue-150: hsl(var(--blue-hsl) / .2);--blue-200: hsl(var(--blue-hsl) / .3);--blue-250: hsl(var(--blue-hsl) / .4);--blue-300: hsl(var(--blue-hsl) / .5);--blue-350: hsl(var(--blue-hsl) / .6);--blue-400: hsl(var(--blue-hsl) / .7);--blue-450: hsl(var(--blue-hsl) / .8);--blue-500: hsl(var(--blue-hsl) / .9);--blue-550: hsl(var(--blue-hsl));--blue-600: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 4%));--blue-650: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 8%));--blue-700: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 12%));--blue-750: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 15%));--blue-800: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 18%));--blue-850: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 24%));--blue-900: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 27%));--purple-h: 246;--purple-s: 90%;--purple-l: 60%;--purple-hsl: var(--purple-h) var(--purple-s) var(--purple-l);--purple: hsl(var(--purple-hsl));--purple-50: hsl(var(--purple-hsl) / .05);--purple-100: hsl(var(--purple-hsl) / .1);--purple-150: hsl(var(--purple-hsl) / .2);--purple-200: hsl(var(--purple-hsl) / .3);--purple-250: hsl(var(--purple-hsl) / .4);--purple-300: hsl(var(--purple-hsl) / .5);--purple-350: hsl(var(--purple-hsl) / .6);--purple-400: hsl(var(--purple-hsl) / .7);--purple-450: hsl(var(--purple-hsl) / .8);--purple-500: hsl(var(--purple-hsl) / .9);--purple-550: hsl(var(--purple-hsl));--purple-600: hsl(var(--purple-h) calc(var(--purple-s) - 4%) calc(var(--purple-l) - 2%));--purple-650: hsl(var(--purple-h) calc(var(--purple-s) - 8%) calc(var(--purple-l) - 4%));--purple-700: hsl(var(--purple-h) calc(var(--purple-s) - 15%) calc(var(--purple-l) - 7%));--purple-750: hsl(var(--purple-h) calc(var(--purple-s) - 23%) calc(var(--purple-l) - 11%));--purple-800: hsl(var(--purple-h) calc(var(--purple-s) - 24%) calc(var(--purple-l) - 15%));--purple-850: hsl(var(--purple-h) calc(var(--purple-s) - 24%) calc(var(--purple-l) - 19%));--purple-900: hsl(var(--purple-h) calc(var(--purple-s) - 27%) calc(var(--purple-l) - 23%));--green-h: 150;--green-s: 80%;--green-l: 42%;--green-hsl: var(--green-h) var(--green-s) var(--green-l);--green: hsl(var(--green-hsl));--green-50: hsl(var(--green-hsl) / .05);--green-100: hsl(var(--green-hsl) / .1);--green-150: hsl(var(--green-hsl) / .2);--green-200: hsl(var(--green-hsl) / .3);--green-250: hsl(var(--green-hsl) / .4);--green-300: hsl(var(--green-hsl) / .5);--green-350: hsl(var(--green-hsl) / .6);--green-400: hsl(var(--green-hsl) / .7);--green-450: hsl(var(--green-hsl) / .8);--green-500: hsl(var(--green-hsl) / .9);--green-550: hsl(var(--green-hsl));--green-600: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 2%));--green-650: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 4%));--green-700: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 8%));--green-750: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 12%));--green-800: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 15%));--green-850: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 19%));--green-900: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 23%));--yellow-h: 38;--yellow-s: 98%;--yellow-l: 64%;--yellow-hsl: var(--yellow-h) var(--yellow-s) var(--yellow-l);--yellow: hsl(var(--yellow-hsl));--yellow-50: hsl(var(--yellow-hsl) / .07);--yellow-100: hsl(var(--yellow-hsl) / .12);--yellow-150: hsl(var(--yellow-hsl) / .2);--yellow-200: hsl(var(--yellow-hsl) / .3);--yellow-250: hsl(var(--yellow-hsl) / .4);--yellow-300: hsl(var(--yellow-hsl) / .5);--yellow-350: hsl(var(--yellow-hsl) / .6);--yellow-400: hsl(var(--yellow-hsl) / .7);--yellow-450: hsl(var(--yellow-hsl) / .8);--yellow-500: hsl(var(--yellow-hsl) / .9);--yellow-550: hsl(var(--yellow-hsl));--yellow-600: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 5%));--yellow-650: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 10%));--yellow-700: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 15%));--yellow-750: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 20%));--yellow-800: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 25%));--yellow-850: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 30%));--yellow-900: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 35%));--red-h: 355;--red-s: 75%;--red-l: 55%;--red-hsl: var(--red-h) var(--red-s) var(--red-l);--red: hsl(var(--red-hsl));--red-50: hsl(var(--red-hsl) / .05);--red-100: hsl(var(--red-hsl) / .1);--red-150: hsl(var(--red-hsl) / .2);--red-200: hsl(var(--red-hsl) / .3);--red-250: hsl(var(--red-hsl) / .4);--red-300: hsl(var(--red-hsl) / .5);--red-350: hsl(var(--red-hsl) / .6);--red-400: hsl(var(--red-hsl) / .7);--red-450: hsl(var(--red-hsl) / .8);--red-500: hsl(var(--red-hsl) / .9);--red-550: hsl(var(--red-hsl));--red-600: hsl(var(--red-h) calc(var(--red-s) - 5%) calc(var(--red-l) - 2%));--red-650: hsl(var(--red-h) calc(var(--red-s) - 10%) calc(var(--red-l) - 4%));--red-700: hsl(var(--red-h) calc(var(--red-s) - 15%) calc(var(--red-l) - 8%));--red-750: hsl(var(--red-h) calc(var(--red-s) - 20%) calc(var(--red-l) - 12%));--red-800: hsl(var(--red-h) calc(var(--red-s) - 25%) calc(var(--red-l) - 15%));--red-850: hsl(var(--red-h) calc(var(--red-s) - 30%) calc(var(--red-l) - 19%));--red-900: hsl(var(--red-h) calc(var(--red-s) - 35%) calc(var(--red-l) - 23%));--codeblock-bg: #f4f4f4}:host(.dark){--gray-s: 15%;--gray-l: 70%;--gray-600: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 6%));--gray-650: hsl(var(--gray-h) calc(var(--gray-s) - 5%) calc(var(--gray-l) + 14%));--gray-700: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 26%));--gray-750: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 36%));--gray-800: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 48%));--gray-850: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 62%));--gray-900: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 70%));--blue-s: 90%;--blue-l: 58%;--blue-600: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 6%));--blue-650: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 12%));--blue-700: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 17%));--blue-750: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 22%));--blue-800: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 28%));--blue-850: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 35%));--blue-900: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 43%));--purple-600: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 4%));--purple-650: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 9%));--purple-700: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 12%));--purple-750: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 18%));--purple-800: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 24%));--purple-850: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 29%));--purple-900: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 33%));--green-600: hsl(calc(var(--green-h) - 1) calc(var(--green-s) - 5%) calc(var(--green-l) + 5%));--green-650: hsl(calc(var(--green-h) - 2) calc(var(--green-s) - 10%) calc(var(--green-l) + 12%));--green-700: hsl(calc(var(--green-h) - 4) calc(var(--green-s) - 15%) calc(var(--green-l) + 20%));--green-750: hsl(calc(var(--green-h) - 6) calc(var(--green-s) - 20%) calc(var(--green-l) + 29%));--green-800: hsl(calc(var(--green-h) - 8) calc(var(--green-s) - 25%) calc(var(--green-l) + 37%));--green-850: hsl(calc(var(--green-h) - 10) calc(var(--green-s) - 30%) calc(var(--green-l) + 42%));--green-900: hsl(calc(var(--green-h) - 12) calc(var(--green-s) - 35%) calc(var(--green-l) + 48%));--yellow-600: hsl(calc(var(--yellow-h) + 1) var(--yellow-s) calc(var(--yellow-l) + 4%));--yellow-650: hsl(calc(var(--yellow-h) + 2) var(--yellow-s) calc(var(--yellow-l) + 7%));--yellow-700: hsl(calc(var(--yellow-h) + 4) var(--yellow-s) calc(var(--yellow-l) + 11%));--yellow-750: hsl(calc(var(--yellow-h) + 6) var(--yellow-s) calc(var(--yellow-l) + 16%));--yellow-800: hsl(calc(var(--yellow-h) + 8) var(--yellow-s) calc(var(--yellow-l) + 20%));--yellow-850: hsl(calc(var(--yellow-h) + 10) var(--yellow-s) calc(var(--yellow-l) + 24%));--yellow-900: hsl(calc(var(--yellow-h) + 12) var(--yellow-s) calc(var(--yellow-l) + 29%));--red-600: hsl(calc(var(--red-h) - 1) calc(var(--red-s) - 5%) calc(var(--red-l) + 3%));--red-650: hsl(calc(var(--red-h) - 2) calc(var(--red-s) - 10%) calc(var(--red-l) + 7%));--red-700: hsl(calc(var(--red-h) - 4) calc(var(--red-s) - 15%) calc(var(--red-l) + 14%));--red-750: hsl(calc(var(--red-h) - 6) calc(var(--red-s) - 20%) calc(var(--red-l) + 19%));--red-800: hsl(calc(var(--red-h) - 8) calc(var(--red-s) - 25%) calc(var(--red-l) + 24%));--red-850: hsl(calc(var(--red-h) - 10) calc(var(--red-s) - 30%) calc(var(--red-l) + 30%));--red-900: hsl(calc(var(--red-h) - 12) calc(var(--red-s) - 35%) calc(var(--red-l) + 36%));--codeblock-bg: var(--gray-100)}", Hs = ":host{--font-family: Inter, system-ui, ui-sans-serif, -apple-system, BlinkMacSystemFont, sans-serif;--monospace-font-family: Inconsolata, Monaco, Consolas, Courier New, Courier,monospace;--font-size-0: .6875rem;--font-size-1: .75rem;--font-size-2: .875rem;--font-size-3: 1rem;--font-size-4: 1.125rem;--font-size-5: 1.25rem;--font-size-6: 1.375rem;--font-size-7: 1.5rem;--line-height-1: 1.125rem;--line-height-2: 1.25rem;--line-height-3: 1.5rem;--line-height-4: 1.75rem;--line-height-5: 2rem;--line-height-6: 2.25rem;--line-height-7: 2.5rem;--font-weight-bold: 500;--font-weight-strong: 600;--font: normal 400 var(--font-size-3) / var(--line-height-3) var(--font-family);--font-bold: normal var(--font-weight-bold) var(--font-size-3) / var(--line-height-3) var(--font-family);--font-strong: normal var(--font-weight-strong) var(--font-size-3) / var(--line-height-3) var(--font-family);--font-small: normal 400 var(--font-size-2) / var(--line-height-2) var(--font-family);--font-small-bold: normal var(--font-weight-bold) var(--font-size-2) / var(--line-height-2) var(--font-family);--font-small-strong: normal var(--font-weight-strong) var(--font-size-2) / var(--line-height-2) var(--font-family);--font-xsmall: normal 400 var(--font-size-1) / var(--line-height-1) var(--font-family);--font-xsmall-bold: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-xsmall-strong: normal var(--font-weight-strong) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-button: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-tooltip: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-2) var(--font-family);--radius-1: .1875rem;--radius-2: .375rem;--radius-3: .75rem;--space-25: 2px;--space-50: 4px;--space-75: 6px;--space-100: 8px;--space-150: 12px;--space-200: 16px;--space-300: 24px;--space-400: 32px;--space-500: 40px;--space-600: 48px;--space-700: 56px;--space-800: 64px;--space-900: 72px;--z-index-component-selector: 100;--z-index-floating-panel: 101;--z-index-drawer: 150;--z-index-spotlight: 200;--z-index-popover: 300;--z-index-activation-button: 1000;--duration-1: .1s;--duration-2: .2s;--duration-3: .3s;--duration-4: .4s}:host{--lumo-font-family: var(--font-family);--lumo-font-size-xs: var(--font-size-1);--lumo-font-size-s: var(--font-size-2);--lumo-font-size-m: var(--font-size-3);--lumo-font-size-l: var(--font-size-4);--lumo-font-size-xl: var(--font-size-5);--lumo-font-size-xxl: var(--font-size-6);--lumo-font-size-xxxl: var(--font-size-7);--lumo-line-height-s: var(--line-height-2);--lumo-line-height-m: var(--line-height-3);--lumo-line-height-l: var(--line-height-4);--lumo-border-radius-s: var(--radius-1);--lumo-border-radius-m: var(--radius-2);--lumo-border-radius-l: var(--radius-3);--lumo-base-color: var(--surface-0);--lumo-body-text-color: var(--color-high-contrast);--lumo-header-text-color: var(--color-high-contrast);--lumo-secondary-text-color: var(--color);--lumo-tertiary-text-color: var(--color);--lumo-error-text-color: var(--color-danger);--lumo-primary-text-color: var(--color-high-contrast);--lumo-primary-color: var(--background-button-primary);--lumo-primary-color-50pct: var(--color-accent);--lumo-space-xs: var(--space-50);--lumo-space-s: var(--space-100);--lumo-space-m: var(--space-200);--lumo-space-l: var(--space-300);--lumo-space-xl: var(--space-500);--lumo-icon-size-xs: var(--font-size-1);--lumo-icon-size-s: var(--font-size-2);--lumo-icon-size-m: var(--font-size-3);--lumo-icon-size-l: var(--font-size-4);--lumo-icon-size-xl: var(--font-size-5)}:host{color-scheme:light;--surface-0: hsl(var(--gray-h) var(--gray-s) 90% / .8);--surface-1: hsl(var(--gray-h) var(--gray-s) 95% / .8);--surface-2: hsl(var(--gray-h) var(--gray-s) 100% / .8);--surface-background: linear-gradient( hsl(var(--gray-h) var(--gray-s) 95% / .7), hsl(var(--gray-h) var(--gray-s) 95% / .65) );--surface-glow: radial-gradient(circle at 30% 0%, hsl(var(--gray-h) var(--gray-s) 98% / .7), transparent 50%);--surface-border-glow: radial-gradient(at 50% 50%, hsl(var(--purple-h) 90% 90% / .8) 0, transparent 50%);--surface: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, hsl(var(--gray-h) var(--gray-s) 98% / .2);--surface-with-border-glow: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, var(--surface-border-glow) no-repeat border-box 0 0 / var(--glow-size, 600px) var(--glow-size, 600px);--surface-border-color: hsl(var(--gray-h) var(--gray-s) 100% / .7);--surface-backdrop-filter: blur(10px);--surface-box-shadow-1: 0 0 0 .5px hsl(var(--gray-h) var(--gray-s) 5% / .15), 0 6px 12px -1px hsl(var(--shadow-hsl) / .3);--surface-box-shadow-2: 0 0 0 .5px hsl(var(--gray-h) var(--gray-s) 5% / .15), 0 24px 40px -4px hsl(var(--shadow-hsl) / .4);--background-button: linear-gradient( hsl(var(--gray-h) var(--gray-s) 98% / .4), hsl(var(--gray-h) var(--gray-s) 90% / .2) );--background-button-active: hsl(var(--gray-h) var(--gray-s) 80% / .2);--color: var(--gray-500);--color-high-contrast: var(--gray-900);--color-accent: var(--purple-700);--color-danger: var(--red-700);--border-color: var(--gray-150);--border-color-high-contrast: var(--gray-300);--border-color-button: var(--gray-350);--border-color-popover: hsl(var(--gray-hsl) / .08);--border-color-dialog: hsl(var(--gray-hsl) / .08);--accent-color: var(--purple-600);--selection-color: hsl(var(--blue-hsl));accent-color:hsl(var(--blue-hsl));--shadow-hsl: var(--gray-h) var(--gray-s) 20%;--lumo-contrast-5pct: var(--gray-100);--lumo-contrast-10pct: var(--gray-200);--lumo-contrast-60pct: var(--gray-400);--lumo-contrast-80pct: var(--gray-600);--lumo-contrast-90pct: var(--gray-800)}:host(.dark){color-scheme:dark;--surface-0: hsl(var(--gray-h) var(--gray-s) 10% / .85);--surface-1: hsl(var(--gray-h) var(--gray-s) 14% / .85);--surface-2: hsl(var(--gray-h) var(--gray-s) 18% / .85);--surface-background: linear-gradient( hsl(var(--gray-h) var(--gray-s) 8% / .65), hsl(var(--gray-h) var(--gray-s) 8% / .7) );--surface-glow: radial-gradient( circle at 30% 0%, hsl(var(--gray-h) calc(var(--gray-s) * 2) 90% / .12), transparent 50% );--surface: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, hsl(var(--gray-h) var(--gray-s) 20% / .4);--surface-border-glow: hsl(var(--gray-h) var(--gray-s) 20% / .4) radial-gradient(at 50% 50%, hsl(250 40% 80% / .4) 0, transparent 50%);--surface-border-color: hsl(var(--gray-h) var(--gray-s) 50% / .2);--surface-box-shadow-1: 0 0 0 .5px hsl(var(--purple-h) 40% 5% / .4), 0 6px 12px -1px hsl(var(--shadow-hsl) / .4);--surface-box-shadow-2: 0 0 0 .5px hsl(var(--purple-h) 40% 5% / .4), 0 24px 40px -4px hsl(var(--shadow-hsl) / .5);--color: var(--gray-650);--background-button: linear-gradient( hsl(var(--gray-h) calc(var(--gray-s) * 2) 80% / .1), hsl(var(--gray-h) calc(var(--gray-s) * 2) 80% / 0) );--background-button-active: hsl(var(--gray-h) var(--gray-s) 10% / .1);--border-color-popover: hsl(var(--gray-h) var(--gray-s) 90% / .1);--border-color-dialog: hsl(var(--gray-h) var(--gray-s) 90% / .1);--shadow-hsl: 0 0% 0%;--lumo-disabled-text-color: var(--lumo-contrast-60pct)}", Fs = "button{-webkit-appearance:none;appearance:none;background:var(--background-button);background-origin:border-box;font:var(--font-button);color:var(--color-high-contrast);border:1px solid var(--border-color);border-radius:var(--radius-2);padding:var(--space-25) var(--space-100)}button:focus-visible{outline:2px solid var(--blue-500);outline-offset:2px}button:active:not(:disabled){background:var(--background-button-active)}button:disabled{color:var(--gray-400);background:transparent}", Gs = ":is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay){z-index:var(--z-index-popover)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay):first-of-type{padding-top:0}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay)::part(overlay){color:inherit;font:inherit;background:var(--surface);-webkit-backdrop-filter:var(--surface-backdrop-filter);backdrop-filter:var(--surface-backdrop-filter);border-radius:var(--radius-2);border:1px solid var(--surface-border-color);box-shadow:var(--surface-box-shadow-1)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay)::part(content){padding:var(--space-50)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item){color:var(--color-high-contrast);font:var(--font-small);display:flex;align-items:center;cursor:default;padding:var(--space-75) var(--space-100);min-height:0;border-radius:var(--radius-1);--_lumo-item-selected-icon-display: none}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled],:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled] .hint,:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled] vaadin-icon{color:var(--lumo-disabled-text-color)}:is(vaadin-context-menu-item,vaadin-menu-bar-item)[expanded]{background:var(--gray-200)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item):not([disabled]):hover{background:var(--color-high-contrast);color:var(--surface-2);--lumo-tertiary-text-color: var(--surface-2);--color: currentColor;--border-color: var(--surface-0)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[focus-ring]{outline:2px solid var(--selection-color);outline-offset:-2px}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item):is([aria-haspopup=true]):after{margin-inline-end:calc(var(--space-200) * -1);margin-right:unset}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item).danger{color:var(--color-danger);--color: currentColor}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item).danger:not([disabled]):hover{background-color:var(--color-danger)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)::part(content){display:flex;align-items:center;gap:var(--space-100)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item) vaadin-icon{width:1em;height:1em;padding:0;color:var(--color)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay) hr{margin:var(--space-50)}:is(vaadin-context-menu-item,vaadin-select-item,vaadin-menu-bar-item) .label{padding-inline-end:var(--space-300)}:is(vaadin-context-menu-item,vaadin-select-item,vaadin-menu-bar-item) .hint{margin-inline-start:auto;color:var(--color)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item) kbd{display:inline-block;border-radius:var(--radius-1);border:1px solid var(--border-color);min-width:1em;min-height:1em;text-align:center;margin:0 .1em;padding:.1em .25em;box-sizing:border-box;font-size:var(--font-size-1);font-family:var(--font-family);line-height:1}.hierarchy-overlay{left:var(--copilot-hierarchy-overlay-left-pos, inherit)!important;top:var(--copilot-hierarchy-overlay-top-pos, inherit)!important}.hierarchy-overlay vaadin-context-menu-item{padding:0}:is(copilot-alignment-overlay)::part(content){padding:0}", Ws = "code.codeblock{background:var(--codeblock-bg);border-radius:var(--radius-2);display:block;font-family:var(--monospace-font-family);font-size:var(--font-size-1);line-height:var(--line-height-1);overflow:hidden;padding:.3125rem 1.75rem .3125rem var(--space-100);position:relative;text-overflow:ellipsis;white-space:pre}copilot-copy{position:absolute;right:0;top:0}copilot-copy button{align-items:center;background:none;border:1px solid transparent;border-radius:var(--radius-2);color:var(--color);display:flex;font:var(--font-button);height:1.75rem;justify-content:center;padding:0;width:1.75rem}copilot-copy button:hover{color:var(--color-high-contrast)}";
var Js = Object.defineProperty, Xs = Object.getOwnPropertyDescriptor, Zs = (e, t, r, n) => {
  for (var i = n > 1 ? void 0 : n ? Xs(t, r) : t, o = e.length - 1, a; o >= 0; o--)
    (a = e[o]) && (i = (n ? a(t, r, i) : a(i)) || i);
  return n && i && Js(t, r, i), i;
};
let bn = class extends us {
  constructor() {
    super(...arguments), this.removers = [], this.initialized = !1, this.overlayOutsideClickListener = (e) => {
      tt(e.target?.owner) || (g.active || tt(e.detail.sourceEvent.target)) && e.preventDefault();
    };
  }
  static get styles() {
    return [
      $e(qs),
      $e(Hs),
      $e(Fs),
      $e(Gs),
      $e(Ws),
      Fa`
        :host {
          position: fixed;
          inset: 0;
          z-index: 9999;
          contain: strict;
          font: var(--font-small);
          color: var(--color);
          pointer-events: all;
        }

        :host(:not([active])) {
          visibility: hidden !important;
          pointer-events: none;
        }

        /* Hide floating panels when not active */

        :host(:not([active])) > copilot-section-panel-wrapper {
          display: none !important;
        }

        /* Keep activation button and menu visible */

        copilot-activation-button,
        .activation-button-menu {
          visibility: visible;
        }

        copilot-activation-button {
          pointer-events: auto;
        }

        a {
          color: var(--blue-600);
          text-decoration-color: var(--blue-200);
        }

        /* Needed to prevent a JS error because of monkey patched '_attachOverlay'. It is some scope issue, */
        /* where 'this._placeholder.parentNode' is undefined - the scope if 'this' gets messed up at some point. */
        /* We also don't want animations on the overlays to make the feel faster, so this is fine. */

        :is(
            vaadin-context-menu-overlay,
            vaadin-menu-bar-overlay,
            vaadin-select-overlay,
            vaadin-combo-box-overlay,
            vaadin-tooltip-overlay
          ):is([opening], [closing]),
        :is(
            vaadin-context-menu-overlay,
            vaadin-menu-bar-overlay,
            vaadin-select-overlay,
            vaadin-combo-box-overlay,
            vaadin-tooltip-overlay
          )::part(overlay) {
          animation: none !important;
        }

        :host(:not([active])) copilot-drawer-panel::before {
          animation: none;
        }

        /* Workaround for https://github.com/vaadin/web-components/issues/5400 */

        :host([active]) .activation-button-menu .activate,
        :host(:not([active])) .activation-button-menu .deactivate,
        :host(:not([active])) .activation-button-menu .toggle-spotlight {
          display: none;
        }
      `
    ];
  }
  connectedCallback() {
    super.connectedCallback(), this.init().catch((e) => Li("Unable to initialize copilot", e));
  }
  async init() {
    if (this.initialized)
      return;
    document.body.style.setProperty("--dev-tools-button-display", "none"), await import("./copilot-global-vars-later-BzktQypt.js"), await import("./copilot-init-step2-BFYaxQGI.js"), fs(), this.tabIndex = 0, bt.hostConnectedCallback(), window.addEventListener("keydown", vn), T.onSend(this.handleSendEvent), this.removers.push(T.on("close-drawers", this.closeDrawers.bind(this))), this.removers.push(
      T.on("open-attention-required-drawer", this.openDrawerIfPanelRequiresAttention.bind(this))
    ), this.addEventListener("mousemove", this.mouseMoveListener), this.addEventListener("dragover", this.mouseMoveListener), Be.addOverlayOutsideClickEvent();
    const e = window.matchMedia("(prefers-color-scheme: dark)");
    this.classList.toggle("dark", e.matches), e.addEventListener("change", (t) => {
      this.classList.toggle("dark", e.matches);
    }), this.reaction(
      () => g.spotlightActive,
      () => {
        he.saveSpotlightActivation(g.spotlightActive), Array.from(this.shadowRoot.querySelectorAll("copilot-section-panel-wrapper")).filter((t) => t.panelInfo?.floating === !0).forEach((t) => {
          g.spotlightActive ? t.style.setProperty("display", "none") : t.style.removeProperty("display");
        });
      }
    ), this.reaction(
      () => g.active,
      () => {
        this.toggleAttribute("active", g.active), g.active ? this.activate() : this.deactivate(), he.saveCopilotActivation(g.active);
      }
    ), this.reaction(
      () => g.activatedAtLeastOnce,
      () => {
        Ii(), ps();
      }
    ), this.reaction(
      () => g.sectionPanelDragging,
      () => {
        g.sectionPanelDragging && Array.from(this.shadowRoot.children).filter((r) => r.localName.endsWith("-overlay")).forEach((r) => {
          r.close && r.close();
        });
      }
    ), he.getCopilotActivation() && Rr().then(() => {
      g.setActive(!0);
    }), this.initialized = !0;
  }
  /**
   * Called when Copilot is activated. Good place to start attach listeners etc.
   */
  activate() {
    Ri("activate"), bt.activate(), _s(), this.openDrawerIfPanelRequiresAttention(), document.documentElement.addEventListener("mouseleave", this.mouseLeaveListener), Be.onCopilotActivation(), T.emit("component-tree-updated", {});
  }
  /**
   * Called when Copilot is deactivated. Good place to remove listeners etc.
   */
  deactivate() {
    this.closeDrawers(), bt.deactivate(), document.documentElement.removeEventListener("mouseleave", this.mouseLeaveListener), Be.onCopilotDeactivation();
  }
  disconnectedCallback() {
    super.disconnectedCallback(), bt.hostDisconnectedCallback(), window.removeEventListener("keydown", vn), T.offSend(this.handleSendEvent), this.removers.forEach((e) => e()), this.removeEventListener("mousemove", this.mouseMoveListener), this.removeEventListener("dragover", this.mouseMoveListener), Be.removeOverlayOutsideClickEvent(), document.documentElement.removeEventListener("vaadin-overlay-outside-click", this.overlayOutsideClickListener);
  }
  handleSendEvent(e) {
    const t = e.detail.command, r = e.detail.data;
    Jt(t, r);
  }
  /**
   * Opens the attention required drawer if there is any.
   */
  openDrawerIfPanelRequiresAttention() {
    const e = Wt.getAttentionRequiredPanelConfiguration();
    if (!e)
      return;
    const t = e.panel;
    if (!t || e.floating)
      return;
    const r = this.shadowRoot.querySelector(`copilot-drawer-panel[position="${t}"]`);
    r.opened = !0;
  }
  render() {
    return pr`
      <copilot-activation-button
        @activation-btn-clicked="${() => {
      g.toggleActive(), g.setLoginCheckActive(!1);
    }}"
        @spotlight-activation-changed="${(e) => {
      g.setSpotlightActive(e.detail);
    }}"
        .spotlightOn="${g.spotlightActive}">
      </copilot-activation-button>
      <copilot-component-selector></copilot-component-selector>
      <copilot-label-editor-container></copilot-label-editor-container>
      <copilot-info-tooltip></copilot-info-tooltip>
      ${this.renderDrawer("left")} ${this.renderDrawer("right")} ${this.renderDrawer("bottom")} ${Ls()}
      <copilot-spotlight ?active=${g.spotlightActive && g.active}></copilot-spotlight>
      <copilot-login-check ?active=${g.loginCheckActive && g.active}></copilot-login-check>
      <copilot-notifications-container></copilot-notifications-container>
    `;
  }
  renderDrawer(e) {
    return pr` <copilot-drawer-panel position=${e}> ${js(e)}</copilot-drawer-panel>`;
  }
  /**
   * Closes the open drawers if any opened unless an overlay is opened from drawer.
   */
  closeDrawers() {
    const e = this.shadowRoot.querySelectorAll(`${et}drawer-panel`);
    if (!Array.from(e).some((o) => o.opened))
      return;
    const r = Array.from(this.shadowRoot.children).find(
      (o) => o.localName.endsWith("overlay")
    ), n = r && Be.getOwner(r);
    if (!n) {
      e.forEach((o) => {
        o.opened = !1;
      });
      return;
    }
    const i = za(n, "copilot-drawer-panel");
    if (!i) {
      e.forEach((o) => {
        o.opened = !1;
      });
      return;
    }
    Array.from(e).filter((o) => o.position !== i.position).forEach((o) => {
      o.opened = !1;
    });
  }
  updated(e) {
    super.updated(e), this.attachActivationButtonToBody(), Ks();
  }
  attachActivationButtonToBody() {
    const e = document.body.querySelectorAll("copilot-activation-button");
    e.length > 1 && e[0].remove();
  }
  mouseMoveListener(e) {
    e.composedPath().find((t) => t.localName === `${et}drawer-panel`) || this.closeDrawers();
  }
  mouseLeaveListener() {
    T.emit("close-drawers", {});
  }
};
bn = Zs([
  Ha("copilot-main")
], bn);
const Ys = window.Vaadin, Qs = {
  init(e) {
    Vi(
      () => window.Vaadin.devTools,
      (t) => {
        const r = t.handleFrontendMessage;
        t.handleFrontendMessage = (n) => {
          zs(n) || r.call(t, n);
        };
      }
    );
  }
};
Ys.devToolsPlugins.push(Qs);
const el = window.litIssuedWarnings ??= /* @__PURE__ */ new Set();
el.add(
  "Multiple versions of Lit loaded. Loading multiple versions is not recommended. See https://lit.dev/msg/multiple-versions for more information."
);
export {
  rl as A,
  bl as B,
  he as C,
  ml as D,
  hl as E,
  ji as F,
  vl as G,
  hs as H,
  _l as I,
  nl as J,
  Ri as K,
  fr as L,
  us as M,
  yi as N,
  pl as O,
  et as P,
  ul as Q,
  A as T,
  il as V,
  bs as a,
  g as b,
  T as c,
  yl as d,
  Wt as e,
  ol as f,
  $e as g,
  Li as h,
  al as i,
  fl as j,
  Fa as k,
  Fe as l,
  wr as m,
  Ps as n,
  gl as o,
  xs as p,
  ss as q,
  ua as r,
  Jt as s,
  Ha as t,
  Ds as u,
  Bs as v,
  Mi as w,
  pr as x,
  ye as y,
  tl as z
};
