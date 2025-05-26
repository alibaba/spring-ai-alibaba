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

import { i18n } from '@/base/i18n'

export class ConfigModel {
  enabled: boolean = true
  hasMatch: boolean = false
  side: string = 'provider'
  matches: any = []
  parameters: any = []
  matchesKeys: any = []
  parametersKeys: any = []
  parametersValue: any = {
    retries: {
      type: 'obj',
      relation: '=',
      value: ''
    },
    timeout: {
      type: 'obj',
      relation: '=',
      value: ''
    },
    accesslog: {
      type: 'obj',
      relation: '=',
      value: ''
    },
    weight: {
      type: 'obj',
      relation: '=',
      value: ''
    },
    other: {
      type: 'free',
      arr: [
        {
          key: '',
          relation: '=',
          value: ''
        }
      ]
    }
  }
  matchesValue: any = {
    address: {
      type: 'obj',
      relation: '',
      value: ''
    },
    providerAddress: {
      type: 'obj',
      relation: '',
      value: ''
    },
    service: {
      type: 'arr',
      arr: [
        {
          key: 'oneof',
          relation: '',
          value: ''
        }
      ]
    },
    application: {
      type: 'arr',
      arr: [
        {
          key: 'oneof',
          relation: '',
          value: ''
        }
      ]
    },
    param: {
      type: 'free',
      arr: [
        {
          key: '',
          relation: '',
          value: ''
        }
      ]
    }
  }

  constructor(obj: any) {
    if (obj) {
      for (let key of Object.keys(this)) {
        if (obj[key]) {
          this[key] = obj[key]
        }
      }
      this.hasMatch = this.matchesKeys.length > 0
    }
  }

  descMatches() {
    let desc = []
    for (let key in this.matchesValue) {
      let tmp = this.matchesValue[key]
      if (!this.matchesKeys.includes(key)) continue
      if (tmp.type === 'obj') {
        desc.push(`${key} ${tmp.relation} ${tmp.value}`)
      } else if (tmp.type === 'arr') {
        let oneof = tmp.arr.map((x) => `${x.relation} ${x.value}`).join(', ')
        desc.push(`${key} oneof [${oneof}]`)
      } else {
        let allof = tmp.arr.map((x) => `${x.key} ${x.relation} ${x.value}`).join(', ')
        desc.push(`${key} allof [${allof}]`)
      }
    }
    return desc
  }

  descParameters() {
    let desc = []
    for (let key in this.parametersValue) {
      let tmp = this.parametersValue[key]
      if (!this.parametersKeys.includes(key)) continue
      if (tmp.type === 'obj') {
        desc.push(`${key} = ${tmp.value}`)
      } else {
        desc.push(...tmp.arr.map((x) => `${x.key} ${x.relation} ${x.value}`))
      }
    }
    return desc
  }

  delArrConfig(obj: any, key: string, idx: number) {
    obj[key].arr.splice(idx, 1)
  }

  addArrConfig(obj: any, key: string, idx: number, val: any | {}) {
    obj[key].arr.splice(idx + 1, 0, {
      key: '',
      relation: val?.relation || '',
      value: ''
    })
  }

  parseMatches(org: any) {
    let obj = this.matchesValue
    for (let key in org) {
      this.matchesKeys.push(key)
      this.hasMatch = true
      let tmp = org[key]
      if (obj[key]) {
        if (obj[key].type === 'obj') {
          for (let tmpKey in tmp) {
            obj[key].relation = tmpKey
            obj[key].value = tmp[tmpKey]
          }
        } else if (obj[key].type === 'arr') {
          obj[key].arr = []
          for (let tmpKey in tmp) {
            for (let tmpItem of tmp[tmpKey]) {
              for (let tmpItemKey in tmpItem) {
                obj[key].arr.push({
                  key: tmpKey,
                  relation: tmpItemKey,
                  value: tmpItem[tmpItemKey]
                })
              }
            }
          }
        } else {
          obj[key].arr = []
          for (let tmpItem of tmp) {
            for (let valueKey in tmpItem.value) {
              obj[key].arr.push({
                key: tmpItem.key,
                relation: valueKey,
                value: tmpItem.value[valueKey]
              })
            }
          }
        }
      }
    }
  }

  parseParameters(org: any) {
    let obj = this.parametersValue
    for (let key in org) {
      let tmp = org[key]
      if (this.parametersValue[key]) {
        this.parametersKeys.push(key)
        obj[key].relation = '='
        obj[key].value = tmp
      } else {
        let key2 = 'other'
        this.parametersKeys.push(key2)
        obj[key2].arr = []
        obj[key2].arr.push({
          key: key,
          relation: '=',
          value: tmp
        })
      }
    }
  }

  checkArrConfig(prefix: string, keys: [any], obj: any, errorMsg: []) {
    for (let key of keys) {
      let item = obj[key]
      if (item.type === 'obj') {
        if (null === item.relation || '' === item.relation) {
          errorMsg.push(`${prefix}: ${key} 条件为空`)
          console.log(`${prefix}: ${key} 条件为空`)
          return false
        }
        if (null === item.value) {
          errorMsg.push(`${prefix}: ${key} 值为空`)
          console.log(`${prefix}: ${key} 值为空`)
          return false
        }
      }
      if (item.type === 'arr') {
        for (let arrElement of item.arr) {
          if (null === arrElement.relation || '' === arrElement.relation) {
            errorMsg.push(`${prefix}: ${key} 条件为空`)
            console.log(`${prefix}: ${key} 条件为空`)
            return false
          }
          if (null === arrElement.value) {
            errorMsg.push(`${prefix}: ${key} 值为空`)
            console.log(`${prefix}: ${key} 值为空`)
            return false
          }
        }
      } else {
        let idx = 1
        if (!item.arr) continue
        for (let arrElement of item.arr) {
          if (null === arrElement.relation || '' === arrElement.relation) {
            errorMsg.push(`${prefix}: ${key} 下第${idx}条记录key为空`)
            console.log(`${prefix}: ${key} 下第${idx}条记录key为空`)
            return false
          }
          if (null === arrElement.relation || '' === arrElement.relation) {
            errorMsg.push(`${prefix}: ${key} 下第${idx}条记录条件为空`)
            console.log(`${prefix}: ${key} 下第${idx}条记录条件为空`)
            return false
          }
          if (null === arrElement.value) {
            errorMsg.push(`${prefix}: ${key} 第${idx}条记录值为空`)
            console.log(`${prefix}: ${key} 第${idx}条记录值为空`)
            return false
          }
          idx++
        }
      }
    }
    return true
  }
}

export class DynamicConfigBasicInfo {
  ruleName: 'org.apache.dubbo.samples.UserService::.configurator'
  scope: '服务'
  configVersion: 'v3.0'
  key: 'org.apache.dubbo.samples.UserService'
  effectTime: '20230/12/19 22:09:34'
  enabled: true
}

export class ViewDataModel {
  basicInfo: DynamicConfigBasicInfo = new DynamicConfigBasicInfo()
  config: ConfigModel[] = []
  errorMsg = []
  isAdd: boolean = false

  constructor() {}

  fromData(data: ViewDataModel) {
    this.basicInfo = data.basicInfo
    this.config = data.config
    this.isAdd = data.isAdd
  }

  fromApiOutput(data: any) {
    this.basicInfo.configVerison = data.configVerison || 'v3.0'
    this.basicInfo.scope = data.scope
    this.basicInfo.key = data.key
    this.basicInfo.enabled = data.enabled || false
    this.config = data.configs.map((x: any) => {
      let configModel = new ConfigModel({
        enabled: x.enabled,
        side: x.side
        // matchesKeys: x.match ? Object.keys(x.match) : [],
        // matches: matches,
        // parametersKeys: Object.keys(x.parameters),
        // parameters: parameters
      })
      configModel.parseMatches(x.match)
      configModel.parseParameters(x.parameters)
      return configModel
    })
  }

  toApiInput(check = false) {
    this.errorMsg = []
    let newVal = {
      ruleName:
        this.basicInfo.ruleName === '_tmp'
          ? this.basicInfo.key + '.configurators'
          : this.basicInfo.ruleName,
      scope: this.basicInfo.scope,
      key: this.basicInfo.key,
      enabled: this.basicInfo.enabled,
      configVersion: this.basicInfo.configVerison || 'v3.0',
      configs: this.config.map((x: configModel, idx: number) => {
        const match: any = {}
        const parameters: any = {}
        if (check) {
          if (x.parametersKeys.length === 0) {
            this.errorMsg.push(
              `配置 ${idx + 1}${i18n.global.t('dynamicConfigDomain.configType')} 不能为空`
            )
            loading.value = false
            throw new Error('数据检查失败')
          }
          if (
            !(
              x.checkArrConfig(
                `配置 ${idx + 1}${i18n.global.t('dynamicConfigDomain.matchType')} 检查失败`,
                x.matchesKeys,
                x.matchesValue,
                this.errorMsg
              ) &&
              x.checkArrConfig(
                `配置 ${idx + 1}${i18n.global.t('dynamicConfigDomain.configType')}  检查失败`,
                x.parametersKeys,
                x.parametersValue,
                this.errorMsg
              )
            )
          ) {
            throw new Error('数据检查失败')
          }
        }
        for (let key of x.matchesKeys) {
          let tmp = x.matchesValue[key]
          if (tmp.type === 'obj') {
            match[key] = { [tmp.relation]: tmp.value }
          } else if (tmp.type === 'arr') {
            match[key] = {
              oneof: tmp.arr.map((xx: any) => {
                return {
                  [xx.relation]: xx.value
                }
              })
            }
          } else {
            match[key] = []
            for (let arrElement of tmp.arr) {
              match[key].push({
                key: arrElement.key,
                value: { [arrElement.relation]: arrElement.value }
              })
            }
          }
        }
        for (let key of x.parametersKeys) {
          let tmp = x.parametersValue[key]
          if (tmp.type === 'obj') {
            parameters[key] = tmp.value
          } else {
            match[key] = {}
            for (let arrElement of tmp.arr) {
              parameters[arrElement.key] = arrElement.value
            }
          }
        }
        return {
          match,
          parameters,
          enabled: x.enabled,
          side: x.side
        }
      })
    }
    return newVal
  }
}
