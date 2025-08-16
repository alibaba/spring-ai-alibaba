/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {reactive} from "vue";

export interface MemoryEmits {
    (e: 'memory-selected'): void
}

export interface InputMessage {
    input: string
    memoryId?: string
    uploadedFiles?: any[]
}

export class MemoryStore {
    // Basic state
    isCollapsed = false
    selectMemoryId = ''
    loadMessages = () => {}
    intervalId: number | undefined = undefined

    toggleSidebar() {
        this.isCollapsed = !this.isCollapsed
        if (this.isCollapsed) {
            this.loadMessages();
            this.intervalId = window.setInterval(() => {
                this.loadMessages();
            }, 3000);
        } else {
            clearInterval(this.intervalId);
        }
    }

    selectMemory(memoryId: string) {
        this.toggleSidebar()
        this.selectMemoryId = memoryId
    }

    setMemory(memoryId: string) {
        this.selectMemoryId = memoryId
    }

    defaultMemoryId() {
        this.selectMemoryId = this.generateRandomId()
    }

    clearMemoryId() {
        this.selectMemoryId = ''
    }

    generateRandomId(): string {
        return Math.random().toString(36).substring(2, 10);
    }

    setLoadMessages(messages : () => void) {
        this.loadMessages = messages
    }
}

export const memoryStore = reactive(new MemoryStore())
