import { IconButton } from '@spark-ai/design';

const defaultTheme = 'dark';

export default function () {
  const theme = prefersColor.get();
  return (
    <IconButton
      bordered={false}
      icon={
        {
          light: 'spark-sun-line',
          dark: 'spark-moon-line',
        }[theme]
      }
      onClick={() => {
        prefersColor.set(theme === 'light' ? 'dark' : 'light');
      }}
      shape="default"
    />
  );
}

export const prefersColor = {
  get() {
    const currentTheme =
      localStorage.getItem('data-prefers-color') || defaultTheme;
    if (currentTheme !== 'light' && currentTheme !== 'dark') {
      localStorage.setItem('data-prefers-color', defaultTheme);
      return defaultTheme;
    } else {
      return currentTheme;
    }
  },
  set(value: 'light' | 'dark') {
    localStorage.setItem('data-prefers-color', value);
    location.reload();
  },
};
