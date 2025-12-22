const charset = [
  ['0', '9'],
  ['a', 'z'],
  ['A', 'Z'],
]
  .map((pair) => pair.map((char) => char.charCodeAt(0)))
  .flatMap(([start, end]) =>
    Array.from({ length: end - start + 1 }, (_, index) => start + index),
  );

const randomId = (length: number): string => {
  return String.fromCharCode.apply(
    String,
    Array.from(
      { length },
      () => charset[Math.floor(Math.random() * charset.length)],
    ),
  );
};

let _seed = 0;
const incrementalId = (length: number): string => {
  _seed += 1;
  return `${_seed.toString(16)}######`.substring(0, length);
};

let _strategy = 'random';
export function setUniqueIdStrategy(strategy: 'incremental' | 'random') {
  _strategy = strategy;
}

export default function uniqueId(length: number): string {
  return _strategy === 'incremental' ? incrementalId(length) : randomId(length);
}
