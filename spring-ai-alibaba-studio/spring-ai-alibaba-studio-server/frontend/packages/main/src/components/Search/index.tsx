import { IconFont, Input } from '@spark-ai/design';
import classNames from 'classnames';
import React from 'react';
import styles from './index.module.less';

interface SearchProps {
  /**
   * Search callback
   */
  onSearch?: (value: string) => void;
  /**
   * Custom className to override default styles
   */
  className?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

/**
 * Knowledge base search component
 * Contains search input and view toggle buttons
 */
const Search: React.FC<SearchProps> = ({
  onSearch,
  className,
  onChange,
  value,
  placeholder = 'Type here...',
}) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    onChange(newValue);
  };

  const handleSearch = () => {
    onSearch?.(value);
  };

  return (
    <Input
      className={classNames(styles['input'], className)}
      prefix={<IconFont type="spark-search-line" />}
      placeholder={placeholder}
      value={value}
      onChange={handleChange}
      onPressEnter={handleSearch}
      allowClear
    />
  );
};

export default Search;
