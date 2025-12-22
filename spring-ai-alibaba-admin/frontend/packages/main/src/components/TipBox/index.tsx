import { Button, IconFont } from '@spark-ai/design';
import React from 'react';
import styles from './index.module.less';

interface LinkButton {
  /** Link text */
  text: string;
  /** Link url */
  url: string;
}

interface Section {
  /** Section title */
  title: string;
  /** Link button list */
  linkButtons: LinkButton[];
  /** Section description */
  description: string;
}

interface TipBoxProps {
  /** Main title */
  title: string;
  /** Description */
  description?: string;
  /** Section list */
  sections: Section[];
}

const TipBox: React.FC<TipBoxProps> = ({ title, description, sections }) => {
  const handleLinkClick = (url: string) => {
    window.open(url, '_blank');
  };

  return (
    <div className={styles['tip-box']}>
      <div className={styles['form-title']}>
        <span className={styles['title-text']}>{title}</span>
      </div>
      {description && (
        <div className={styles['description']}>{description}</div>
      )}

      {sections.map((section, index) => (
        <div key={index} className={styles['section-container']}>
          <div className={styles['section-content']}>
            <div className={styles['bullet-point']}></div>
            <div className={styles['section-header']}>
              <span className={styles['section-title']}>{section.title}</span>
              {section.linkButtons.map((linkButton, linkIndex) => (
                <Button
                  key={linkIndex}
                  type="link"
                  size="small"
                  className={styles['link-button']}
                  onClick={() => handleLinkClick(linkButton.url)}
                >
                  {linkButton.text}
                  <IconFont
                    type="spark-upperrightArrow-line"
                    className={styles['link-icon']}
                  />
                </Button>
              ))}
            </div>
            <div className={styles['section-description']}>
              {section.description}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default TipBox;
