/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { nextTick, ref } from 'vue'
import _ from 'lodash'

export class ScrollController {
  constructor() {}

  scrollContainer = ref<Element | any>(null)
  autoScroll = ref<boolean>(true)

  scrollToPosition(distance: number, handle: any, duration = 500) {
    let startTime: number | null = null
    let self = this

    function animation(currentTime: number) {
      if (!self.autoScroll.value) return
      if (startTime === null) startTime = currentTime
      const timeElapsed = currentTime - startTime
      const progress = Math.min(timeElapsed / 3 / duration, 1)
      const easeProgress = easeInOutQuad(progress)
      handle(distance * easeProgress)
      if (progress < 1) requestAnimationFrame(animation)
    }

    // 二次方缓动函数
    function easeInOutQuad(t: number) {
      return t < 0.5 ? 1.2 * t * t : -1 + (3 - 2 * t) * t
    }

    requestAnimationFrame(animation)
  }

  scrollToBottom() {
    _.debounce(() => {
      if (!this.scrollContainer.value) return
      nextTick(() => {
        this.scrollToPosition(this.scrollContainer.value.scrollHeight, (dis: number) => {
          this.scrollContainer.value.scrollTop += dis
        })
      })
    }, 1000)()
  }

  lastScrollTop = 0

  handleScroll() {
    if (!this.scrollContainer?.value) return
    const { scrollTop, scrollHeight, clientHeight } = this.scrollContainer.value
    let now = scrollTop + clientHeight
    if (this.autoScroll.value && scrollTop < this.lastScrollTop) {
      this.autoScroll.value = false
    } else if (!this.autoScroll.value && now >= scrollHeight - 50) {
      this.autoScroll.value = true
    }
    this.lastScrollTop = scrollTop
  }

  init(scrollContainer: Element | any) {
    if (this.scrollContainer?.value) return
    this.scrollContainer = scrollContainer
    if (this.scrollContainer?.value) {
      this.scrollToBottom()
      this.scrollContainer.value.addEventListener('scroll', () => {
        this.handleScroll()
      })
    }
  }

  fresh() {
    if (this.autoScroll.value) {
      this.scrollToBottom()
    }
  }
}
