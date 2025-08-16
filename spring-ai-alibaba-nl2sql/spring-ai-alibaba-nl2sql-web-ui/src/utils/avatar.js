/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const PROFESSIONAL_STYLES = [
  'avataaars',
  'adventurer-neutral',
  'big-ears-neutral',
  'lorelei-neutral',
  'personas',
  'micah',
  'initials'
]

// 现代配色方案
const MODERN_COLORS = [
  'blue', 'indigo', 'purple', 'teal', 'green', 
  'amber', 'orange', 'red', 'pink', 'gray'
]

/**
 * 生成随机专业头像
 * @param {string} seed - 种子值，用于确保相同输入产生相同头像
 * @returns {string} 头像URL
 */
export function generateProfessionalAvatar(seed = null) {
  const actualSeed = seed || Math.random().toString(36).substring(2, 15)
  
  // 基于种子选择风格和颜色，确保一致性
  const styleIndex = hashString(actualSeed) % PROFESSIONAL_STYLES.length
  const colorIndex = hashString(actualSeed + 'color') % MODERN_COLORS.length
  
  const style = PROFESSIONAL_STYLES[styleIndex]
  const color = MODERN_COLORS[colorIndex]
  
  // 为不同风格设置不同的参数以确保专业外观
  let styleParams = ''
  if (style === 'avataaars') {
    styleParams = '&accessories=prescription02,round,sunglasses,wayfarers&accessoriesColor=black,blue,brown,gray&clothingColor=blue,gray,heather,pastel&eyebrowType=default,raised&eyeType=default,happy,squint&facialHairType=blank,light&hairColor=auburn,black,blonde,brown,gray,red&hatColor=black,blue,gray,heather,pastel&mouthType=default,smile&skinColor=light,yellow,pale,dark'
  } else if (style === 'initials') {
    styleParams = `&backgroundColor=${color}&fontSize=50&fontWeight=600&textColor=white`
  } else {
    styleParams = `&backgroundColor=${color}`
  }
  
  return `https://api.dicebear.com/7.x/${style}/svg?seed=${actualSeed}&size=200${styleParams}`
}

/**
 * 基于智能体ID生成一致的头像
 * @param {string} agentId - 智能体ID
 * @returns {string} 头像URL
 */
export function getAgentAvatar(agentId) {
  return generateProfessionalAvatar(agentId)
}

/**
 * 生成新的随机头像
 * @returns {string} 头像URL
 */
export function generateRandomAvatar() {
  return generateProfessionalAvatar()
}

/**
 * 简单的字符串哈希函数
 * @param {string} str - 输入字符串
 * @returns {number} 哈希值
 */
function hashString(str) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // 转换为32位整数
  }
  return Math.abs(hash)
}

/**
 * 验证头像URL是否有效
 * @param {string} url - 头像URL
 * @returns {boolean} 是否有效
 */
export function isValidAvatarUrl(url) {
  if (!url || typeof url !== 'string') return false
  
  // 检查是否是有效的URL格式
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 获取头像显示URL，如果自定义头像无效则使用默认头像
 * @param {string} customAvatar - 自定义头像URL
 * @param {string} agentId - 智能体ID（用于生成一致的默认头像）
 * @returns {string} 最终显示的头像URL
 */
export function getDisplayAvatar(customAvatar, agentId) {
  if (isValidAvatarUrl(customAvatar)) {
    return customAvatar
  }
  return getAgentAvatar(agentId)
}
