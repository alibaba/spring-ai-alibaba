/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useState } from 'react';
import logo from '@/assets/logo.png';
import styles from './index.module.css';

export default function IndexPage() {
  const [count, setCount] = useState(1);
  const updateCount = () => setCount((c) => c + 1);

  return (
    <div className={styles.app}>
      <header>
        <img src={logo} alt="logo" />
        <p>
          Hello ice.js 3
        </p>
      </header>
      <main>
        <button className={styles.button} type="button" onClick={updateCount}>
          👍🏻 {count}
        </button>
        <p>
          <a
            href="https://reactjs.org"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.link}
          >
            Learn React
          </a>
          {' | '}
          <a
            href="https://v3.ice.work/"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.link}
          >
            Learn ice.js
          </a>
        </p>
      </main>
    </div>
  );
}
