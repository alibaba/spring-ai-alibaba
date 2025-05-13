import fs from 'fs';
import yaml from 'js-yaml';
import merge from 'lodash.merge';

import type { MetaData } from '@/types';

export interface SiteConfig {
  name: string;
  site?: string;
  base?: string;
  trailingSlash?: string;
  githubUrl?: string;
  websiteGithubUrl?: string;
  logoUrl?: string;
  downloadEbooks?: string;
}
export interface MetaDataConfig extends Omit<MetaData, 'title'> {
  title?: {
    default: string;
    template: string;
  };
}
export interface AlgoliaConfig {
  appId: string;
  apiKey: string;
  indexName: string;
}
export interface AnalyticsConfig {
  vendors: {
    googleAnalytics: {
      id?: string;
      partytown?: boolean;
    };
    baiduAnalytics: {
      id?: string;
      verification?: string;
    };
    aesAnalytics: {
      pid?: string;
    };
    clarityAlalytics: {
      id?: string;
    }
  };
}

const config = yaml.load(fs.readFileSync('src/config.yaml', 'utf8')) as {
  site?: SiteConfig;
  metadata?: MetaDataConfig;
  algolia?: AlgoliaConfig;
  ui?: unknown;
  analytics?: unknown;
};

const DEFAULT_SITE_NAME = 'Spring AI Alibaba';

const getSite = () => {
  const _default = {
    name: DEFAULT_SITE_NAME,
    site: undefined,
    base: '/',
    trailingSlash: false,
  };

  return merge({}, _default, config?.site ?? {}) as SiteConfig;
};

const getMetadata = () => {
  const siteConfig = getSite();

  const _default = {
    title: {
      default: siteConfig?.name || DEFAULT_SITE_NAME,
      template: '%s',
    },
    description: '',
    robots: {
      index: false,
      follow: false,
    },
    openGraph: {
      type: 'website',
    },
  };

  return merge({}, _default, config?.metadata ?? {}) as MetaDataConfig;
};

const getAlgolia = () => {
  const _default = {
    appId: '1QV814950M',
    apiKey: '7445da3dec050d45d29f3fe93ed45af3',
    indexName: 'nacos',
}

  return merge({}, _default, config?.algolia ?? {}) as AlgoliaConfig;
};


const getUI = () => {
  const _default = {
    colors: {},
  };

  return merge({}, _default, config?.ui ?? {});
};

const getAnalytics = () => {
  const _default = {
    vendors: {
      googleAnalytics: {
        id: undefined,
        partytown: true,
      },
    },
  };

  return merge({}, _default, config?.analytics ?? {}) as AnalyticsConfig;
};

export const SITE = getSite();
export const ALGOLIA = getAlgolia();
export const METADATA = getMetadata();
export const UI = getUI();
export const ANALYTICS = getAnalytics();
