# rpm远程手机设备管理系统



# 客户端

## 	小慧定制 - 手机管理应用

### 		应用介绍

​				小慧定制是一款专为特定需求设计的手机管理应用，通过提供锁屏、时间管理等功能，帮助用户更好地控制手机使用时间，提高工作和学习效率。

### 		主要功能

#### 	1. 锁屏管理

- ​		**密码锁屏**: 生成随机6位数密码，实现安全锁定
- ​		**远程解锁**: 支持远程解锁功能，便于管理
- ​		**紧急解锁**: 提供每日有限次数(3次)的紧急解锁功能

#### 	2. 番茄工作法

- **内置番茄时钟**: 帮助用户专注工作和学习
- **时间统计**: 记录专注时间，提供使用报告

#### 	3. 设备管理

- **设备信息**: 收集并显示设备基本信息
- **锁定记录**: 记录设备锁定历史

#### 	4. 用户体验

- **启动页**: 提供优雅的应用启动体验
- **安全防护**: 多层次锁定机制，防止意外退出

### 技术特点

- 使用Kotlin和Jetpack Compose构建现代化UI
- 采用MVVM架构设计
- 依赖注入(Hilt)实现组件解耦
- 使用设备管理员API实现锁屏控制
- 协程处理异步任务

### 权限说明

​	应用需要以下权限以确保正常功能：

- ​		设备管理员权限：用于控制锁屏
- ​		任务管理权限：保持锁屏状态
- ​		开机自启动权限：确保锁定策略在重启后生效

### 	使用说明

1. ​		首次使用时，请授予应用所需权限
2. ​		锁屏后，需要输入正确密码或通过远程解锁才能解除锁定
3. ​		紧急情况下可使用紧急解锁功能（每日限额3次）
4. ​		番茄时钟可设置工作时间和休息时间，帮助您保持专注

### 	开发者

​		开发与定制服务由幻兰乜提供



# 后端

<p align="center">
	<img alt="logo" src="https://oscimg.oschina.net/oscnet/up-dd77653d7c9f197dd9d93684f3c8dcfbab6.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">RuoYi v4.8.0</h1>
<h4 align="center">基于SpringBoot开发的轻量级Java快速开发框架</h4>
<p align="center">
	<a href="https://gitee.com/y_project/RuoYi/stargazers"><img src="https://gitee.com/y_project/RuoYi/badge/star.svg?theme=gvp"></a>
	<a href="https://gitee.com/y_project/RuoYi"><img src="https://img.shields.io/badge/RuoYi-v4.8.0-brightgreen.svg"></a>
	<a href="https://gitee.com/y_project/RuoYi/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

演示地址：http://ruoyi.vip  
文档地址：http://doc.ruoyi.vip
