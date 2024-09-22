/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/// <reference lib="es2018" />
import { Flow as _Flow } from "Frontend/generated/jar-resources/Flow.js";
import React, { useCallback, useEffect, useRef } from "react";
import {
    matchRoutes,
    useBlocker,
    useLocation,
    useNavigate
} from "react-router-dom";
import type { AgnosticRouteObject } from '@remix-run/router';

const flow = new _Flow({
    imports: () => import("Frontend/generated/flow/generated-flow-imports.js")
});

const router = {
    render() {
        return Promise.resolve();
    }
};

// ClickHandler for vaadin-router-go event is copied from vaadin/router click.js
// @ts-ignore
function getAnchorOrigin(anchor) {
    // IE11: on HTTP and HTTPS the default port is not included into
    // window.location.origin, so won't include it here either.
    const port = anchor.port;
    const protocol = anchor.protocol;
    const defaultHttp = protocol === 'http:' && port === '80';
    const defaultHttps = protocol === 'https:' && port === '443';
    const host = (defaultHttp || defaultHttps)
        ? anchor.hostname // does not include the port number (e.g. www.example.org)
        : anchor.host; // does include the port number (e.g. www.example.org:80)
    return `${protocol}//${host}`;
}

function normalizeURL(url: URL): void | string {
    // ignore click if baseURI does not match the document (external)
    if (!url.href.startsWith(document.baseURI)) {
        return;
    }

    // Normalize path against baseURI
    return '/' + url.href.slice(document.baseURI.length);
}

function extractPath(event: MouseEvent): void | string {
    // ignore the click if the default action is prevented
    if (event.defaultPrevented) {
        return;
    }

    // ignore the click if not with the primary mouse button
    if (event.button !== 0) {
        return;
    }

    // ignore the click if a modifier key is pressed
    if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) {
        return;
    }

    // find the <a> element that the click is at (or within)
    let maybeAnchor = event.target;
    const path = event.composedPath
        ? event.composedPath()
        // @ts-ignore
        : (event.path || []);

    // example to check: `for...of` loop here throws the "Not yet implemented" error
    for (let i = 0; i < path.length; i++) {
        const target = path[i];
        if (target.nodeName && target.nodeName.toLowerCase() === 'a') {
            maybeAnchor = target;
            break;
        }
    }

    // @ts-ignore
    while (maybeAnchor && maybeAnchor.nodeName.toLowerCase() !== 'a') {
        // @ts-ignore
        maybeAnchor = maybeAnchor.parentNode;
    }

    // ignore the click if not at an <a> element
    // @ts-ignore
    if (!maybeAnchor || maybeAnchor.nodeName.toLowerCase() !== 'a') {
        return;
    }

    const anchor = maybeAnchor as HTMLAnchorElement;

    // ignore the click if the <a> element has a non-default target
    if (anchor.target && anchor.target.toLowerCase() !== '_self') {
        return;
    }

    // ignore the click if the <a> element has the 'download' attribute
    if (anchor.hasAttribute('download')) {
        return;
    }

    // ignore the click if the <a> element has the 'router-ignore' attribute
    if (anchor.hasAttribute('router-ignore')) {
        return;
    }

    // ignore the click if the target URL is a fragment on the current page
    if (anchor.pathname === window.location.pathname && anchor.hash !== '') {
        // @ts-ignore
        window.location.hash = anchor.hash;
        return;
    }

    // ignore the click if the target is external to the app
    // In IE11 HTMLAnchorElement does not have the `origin` property
    // @ts-ignore
    const origin = anchor.origin || getAnchorOrigin(anchor);
    if (origin !== window.location.origin) {
        return;
    }

    return normalizeURL(new URL(anchor.href, anchor.baseURI));
}

/**
 * Fire 'vaadin-navigated' event to inform components of navigation.
 * @param pathname pathname of navigation
 * @param search search of navigation
 */
function fireNavigated(pathname:string, search: string) {
    setTimeout(() =>
        window.dispatchEvent(new CustomEvent('vaadin-navigated', {
            detail: {
                pathname,
                search
            }
        }))
    )
}

function postpone() {
}

const prevent = () => postpone;

type RouterContainer = Awaited<ReturnType<typeof flow.serverSideRoutes[0]["action"]>>;

function Flow() {
    const ref = useRef<HTMLOutputElement>(null);
    const navigate = useNavigate();
    const blocker = useBlocker(({ currentLocation, nextLocation }) => {
        navigated.current = navigated.current || (nextLocation.pathname === currentLocation.pathname && nextLocation.search === currentLocation.search && nextLocation.hash === currentLocation.hash);
        return true;
    });
    const {pathname, search, hash} = useLocation();
    const navigated = useRef<boolean>(false);

    const containerRef = useRef<RouterContainer | undefined>(undefined);

    const navigateEventHandler = useCallback((event: MouseEvent) => {
        const path = extractPath(event);
        if (!path) {
            return;
        }

        if (event && event.preventDefault) {
            event.preventDefault();
        }

        navigated.current = false;
        navigate(path);
    }, [navigate]);

    const vaadinRouterGoEventHandler = useCallback((event: CustomEvent<URL>) => {
        const url = event.detail;
        const path = normalizeURL(url);
        if (!path) {
            return;
        }

        event.preventDefault();
        navigate(path);
    }, [navigate]);

    const vaadinNavigateEventHandler = useCallback((event: CustomEvent<{state: unknown, url: string, replace?: boolean}>) => {
        const path = '/' + event.detail.url;
        navigated.current = !event.detail.replace;
        navigate(path, { state: event.detail.state, replace: event.detail.replace});
    }, [navigate]);

    const redirect = useCallback((path: string) => {
        return (() => {
            navigate(path, {replace: true});
        });
    }, [navigate]);

    useEffect(() => {
        // @ts-ignore
        window.addEventListener('vaadin-router-go', vaadinRouterGoEventHandler);
        // @ts-ignore
        window.addEventListener('vaadin-navigate', vaadinNavigateEventHandler);

        return () => {
            // @ts-ignore
            window.removeEventListener('vaadin-router-go', vaadinRouterGoEventHandler);
            // @ts-ignore
            window.removeEventListener('vaadin-navigate', vaadinNavigateEventHandler);
        };
    }, [vaadinRouterGoEventHandler, vaadinNavigateEventHandler]);

    useEffect(() => {
        return () => {
            containerRef.current?.parentNode?.removeChild(containerRef.current);
            containerRef.current = undefined;
        };
    }, []);

    useEffect(() => {
        if (blocker.state === 'blocked') {
            if(navigated.current) {
                blocker.proceed();
                return;
            }
            const {pathname, search} = blocker.location;
            const routes = ((window as any)?.Vaadin?.routesConfig || []) as AgnosticRouteObject[];
            let matched = matchRoutes(Array.from(routes), window.location.pathname);

            // Navigation between server routes
            // @ts-ignore
            if (matched && matched.filter(path => path.route?.element?.type?.name === Flow.name).length != 0) {
                containerRef.current?.onBeforeEnter?.call(containerRef?.current,
                    {pathname,search}, {
                        prevent() {
                            blocker.reset();
                            navigated.current = false;
                        },
                        redirect,
                        continue() {
                            blocker.proceed();
                        }
                    }, router);
                navigated.current = true;
            } else {
                // For covering the 'server -> client' use case
                Promise.resolve(containerRef.current?.onBeforeLeave?.call(containerRef?.current, {
                    pathname,
                    search
                }, {prevent}, router))
                    .then((cmd: unknown) => {
                        if (cmd === postpone && containerRef.current) {
                            // postponed navigation: expose existing blocker to Flow
                            containerRef.current.serverConnected = (cancel) => {
                                if (cancel) {
                                    blocker.reset();
                                } else {
                                    blocker.proceed();
                                    window.removeEventListener('click',  navigateEventHandler);
                                }
                            }
                        } else {
                            // permitted navigation: proceed with the blocker
                            blocker.proceed();
                            window.removeEventListener('click',  navigateEventHandler);
                        }
                    });
            }
        }
    }, [blocker.state, blocker.location]);

    useEffect(() => {
        if(navigated.current) {
            navigated.current = false;
            fireNavigated(pathname,search);
            return;
        }
        flow.serverSideRoutes[0].action({pathname, search})
            .then((container) => {
                const outlet = ref.current?.parentNode;
                if (outlet && outlet !== container.parentNode) {
                    outlet.append(container);
                    window.addEventListener('click',  navigateEventHandler);
                    containerRef.current = container
                }
                return container.onBeforeEnter?.call(container, {pathname, search}, {prevent, redirect, continue() {
                        fireNavigated(pathname,search);}}, router);
            })
            .then((result: unknown) => {
                if (typeof result === "function") {
                    result();
                }
            });
    }, [pathname, search, hash]);

    return <output ref={ref} />;
}
Flow.type = 'FlowContainer'; // This is for copilot to recognize this

export const serverSideRoutes = [
    { path: '/*', element: <Flow/> },
];

/**
 * Load the script for an exported WebComponent with the given tag
 *
 * @param tag name of the exported web-component to load
 *
 * @returns Promise(resolve, reject) that is fulfilled on script load
 */
export const loadComponentScript = (tag: String): Promise<void> => {
    return new Promise((resolve, reject) => {
        useEffect(() => {
            const script = document.createElement('script');
            script.src = `/web-component/${tag}.js`;
            script.onload = function() {
                resolve();
            };
            script.onerror = function(err) {
                reject(err);
            };
            document.head.appendChild(script);

            return () => {
                document.head.removeChild(script);
            }
        }, []);
    });
};

interface Properties {
    [key: string]: string;
}

/**
 * Load WebComponent script and create a React element for the WebComponent.
 *
 * @param tag custom web-component tag name.
 * @param props optional Properties object to create element attributes with
 * @param onload optional callback to be called for script onload
 * @param onerror optional callback for error loading the script
 */
export const reactElement = (tag: string, props?: Properties, onload?: () => void, onerror?: (err:any) => void) => {
    loadComponentScript(tag).then(() => onload?.(), (err) => {
        if(onerror) {
            onerror(err);
        } else {
            console.error(`Failed to load script for ${tag}.`, err);
        }
    });

    if(props) {
        return React.createElement(tag, props);
    }
    return React.createElement(tag);
};

export default Flow;

// @ts-ignore
if (import.meta.hot) {
  // @ts-ignore
  import.meta.hot.accept((newModule) => {
    // A hot module replace for Flow.tsx happens when any JS/TS imported through @JsModule
    // or similar is updated because this updates generated-flow-imports.js and that in turn
    // is imported by this file. We have no means of hot replacing those files, e.g. some
    // custom lit element so we need to reload the page. */
    if (newModule) {
      window.location.reload();
    }
  });
}
