import { InputNumber, Slider } from '@spark-ai/design';
import { Flex, SliderSingleProps } from 'antd';
import classNames from 'classnames';
import React from 'react';
import styles from './index.module.less';

interface IProps extends SliderSingleProps {
  isShowMarker?: boolean;
}

export const SliderInput: React.FC<IProps> = (props) => {
  const {
    value,
    onChange,
    step,
    min,
    max,
    className,
    isShowMarker,
    style,
    ...rest
  } = props;

  const handleChange = (value: number | null) => {
    if (value !== null) {
      onChange?.(value);
    }
  };

  return (
    <div className={classNames(styles['slider-input'], className)}>
      <Flex vertical>
        <Slider
          value={value}
          onChange={onChange}
          step={step}
          min={min}
          max={max}
          style={style}
          {...rest}
        />
        {isShowMarker && (
          <Flex
            align="center"
            justify="space-between"
            className={styles['slider-marker']}
            style={style}
          >
            <div className={styles['slider-marker']}>{min}</div>
            <div className={styles['slider-marker']}>{max}</div>
          </Flex>
        )}
      </Flex>
      <InputNumber
        step={step || 0.01}
        min={min}
        max={max}
        value={value}
        onChange={handleChange}
        formatter={(value) =>
          Number.isInteger(step) ? `${Math.floor(Number(value))}` : `${value}`
        }
        parser={(value) =>
          Number.isInteger(step)
            ? Math.floor(Number(value || 0))
            : Number(value || 0)
        }
      />
    </div>
  );
};

export default SliderInput;
