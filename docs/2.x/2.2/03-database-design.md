# v2.2 数据库设计

状态：已完成

增量脚本：sql/ticket-v2.2.sql

ticket_attachment 保存 attachment_id、ticket_id、comment_id、business_type、bind_status、original_name、storage_path、content_type、extension、file_size、uploader_id、delete_status、cleanup_status 及审计时间。

关键索引覆盖工单查询、评论查询、上传人临时附件和待清理附件。数据库只保存相对路径，不保存文件二进制和绝对路径。
