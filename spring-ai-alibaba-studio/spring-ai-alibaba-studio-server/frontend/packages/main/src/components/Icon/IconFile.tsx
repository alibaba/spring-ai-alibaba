import React from 'react';

import type { FileType } from '@/types/base';

const pdfLogo = '/images/pdf.svg';
const mdLogo = '/images/md.svg';
const excelLogo = '/images/excel.svg';
const pptLogo = '/images/ppt.svg';
const txtLogo = '/images/txt.png';
const wordLogo = '/images/word.png';
const GeneralLogo = '/images/general.png';

interface IconFileProps {
  className?: string;
  type: FileType;
}

const IconFile: React.FC<IconFileProps> = ({ type, className }) => {
  const iconMap = {
    PDF: pdfLogo,
    MD: mdLogo,
    Excel: excelLogo,
    PPT: pptLogo,
    TXT: txtLogo,
    DOC: wordLogo,
    DOCX: wordLogo,
    PPTX: pptLogo,
    GENERAL: GeneralLogo,
  };
  const iconSrc = iconMap[type] || iconMap.GENERAL;
  return <img src={iconSrc} className={className} />;
};

export default IconFile;
