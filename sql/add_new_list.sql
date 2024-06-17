use yubi; -- 换成后端的数据库名字来着
ALTER TABLE chart ADD contentId BIGINT;

-- 如果确保 content_id 字段唯一
ALTER TABLE chart ADD UNIQUE (contentId);