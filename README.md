# pcr-wiki-mirai
基于[mirai-console](https://github.com/mamoe/mirai-console) 平台的 Princess Connect Re:Dive WIKI插件
* 当前版本基于[mirai-console](https://github.com/mamoe/mirai-console) 0.5.2

## 已实现功能及指令
* 查询角色简介（在聊天群内输入 @Bot角色简介（空格）角色名)
* 查询角色详情（在聊天群内输入 @Bot角色详情（空格）角色名）
* 查询角色技能（在聊天群内输入 @Bot角色技能（空格）角色名）

## 将来计划实现的功能
* 查询角色装备以及需求
* 查询装备刷取地图
* 会战boss数据查询
* ...

## 部署说明
* 将本插件的jar包放置于mirai根目录下的 plugins 文件夹
* 随后可在plugins目录下手动新建 pcr-wiki 文件夹，或者启动一次插件后自动生成该文件夹
* 将_pcr_data.json文件放入 plugins 文件夹
* 手动新建setting.yml文件放入 plugins 文件夹，或者随后由插件自动生成

## 配置文件
* 用户可编辑 setting.yml 文件，对插件进行一些配置
* 在文件内写入 location:'CN' 则插件自动获取简体字服数据库，默认配置为'CN'
* 在文件内写入 location:'JP' 则插件自动获取日语服数据库
* 在文件内写入 autoUpdate:true 则插件会在启动的24小时后主动检查数据库版本并更新，此项默认开启
* 在文件内写入 autoUpdate:false 则插件关闭自动检查数据库版本的功能

## _pcr_data.json文件的获取
* 这个文件来自[Hoshinobot](https://github.com/Ice-Cirno/HoshinoBot) 的_pcr_data.py文件
* 可直接由本项目relese中下载
* 或者按如下方法手动获取:
    * 获取[Hoshinobot](https://github.com/Ice-Cirno/HoshinoBot) ，打开..\hoshino\modules\priconne，
    找到 _pcr_data.py 文件，并复制到自己能够找到的文件夹
    * 用记事本（推荐notepad++）打开刚刚复制的 _pcr_data.py 文件，在首行输入 
    ```import json```,
    在尾行输入
    ```
        with open("_pcr_data.json",'w',encoding='utf-8') as f:
            json.dump(CHARA_NAME,f,ensure_ascii=False)
    ```
  保存，运行_pcr_data.py 文件，之后便能在相同的文件夹找到_pcr_data.json 文件

* 新增角色时，用户可自行获取最新的 _pcr_data.py 文件并按以上方法编辑运行，或自行编辑已有的.json 文件
手动添加
* 关于如何运行Python程序，本教程不会赘述，请自行搜索  
    
    

## 注意
* 基于[mirai-console](https://github.com/mamoe/mirai-console) 0.5.2 的版本将不会再新增功能，因为[mirai-console](https://github.com/mamoe/mirai-console) 1.0
即将发布，鉴于API变化过大，本项目首先会以适配1.0版本为主。

## 感谢
* 本项目特别感谢[KasumiNotes](https://github.com/HerDataSam/KasumiNotes) 给予的灵感以及参考，[KasumiNotes](https://github.com/HerDataSam/KasumiNotes) 是一款综合类
Princess Connect Re:Dive WIKI 安卓APP
* 特别感谢[Mirai](https://github.com/mamoe/mirai) 以及[mirai-console](https://github.com/mamoe/mirai-console) 的平台支持
