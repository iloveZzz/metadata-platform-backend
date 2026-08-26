-- V3 回滚：删除 dq_channel 表（切片 04 通道管理；无历史结果引用时方可回滚）
DROP TABLE IF EXISTS dq_channel;
