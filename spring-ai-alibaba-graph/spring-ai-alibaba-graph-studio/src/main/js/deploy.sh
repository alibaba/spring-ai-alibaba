bun build.sh
bun twgen.ts --no-watch
bun build.sh
rm ../resources/webapp/*
cp dist/* ../resources/webapp