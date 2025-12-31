import React, { memo, useState } from 'react';

export default memo(function IteratorPanel() {
  const [state, setState] = useState({
    loading: false,
  });
  return <div>IteratorPanel</div>;
});
