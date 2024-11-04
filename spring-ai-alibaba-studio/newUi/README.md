# A new ice.js project

## Usage

```bash
$ npm install

$ npm start
```

## Directories

```md
.
├── README.md
├── ice.config.mts                  # The project config.
├── package.json
├── .browserslistrc                 # Browsers that support.
├── public                          # Static files.
├── src                             # Application source code.
│   ├── app.ts                      # The app entry.
│   ├── assets                      # Assets directory.
│   ├── document.tsx                # The document entry.
│   ├── components                  # Components directory.
│   ├── pages                       # Pages directory.
│   │   ├── index.module.css        # Index page style.
│   │   └── index.tsx               # Index page component.
│   └── typings.d.ts                # The type definition.
└── tsconfig.json
```

> Note: The resources in `public` directory will be completely copied to the `output` directory during the build phase, and the filename will not be changed.

For more detail, please visit [docs](https://v3.ice.work/).
