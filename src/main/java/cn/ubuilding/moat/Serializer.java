package cn.ubuilding.moat;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * @author Wu Jianfeng
 * @since 15/12/25 08:10
 * 序列化工具使用 Protostuff实现
 */

public final class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T t) {
        if (null == t) throw new IllegalArgumentException("the serialized object must not be null");
        Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(t.getClass());
        LinkedBuffer buffer = LinkedBuffer.allocate(4096);// TODO 固定分配大小是否合适
        try {
            return ProtostuffIOUtil.toByteArray(t, schema, buffer);
        } catch (Exception e) {
            logger.error("[Serializer] serialize error:" + e.getMessage());
            throw new RuntimeException("serialization exception:" + e.getMessage());
        } finally {
            buffer.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> byte[] serializeList(List<T> list) {
        if (null == list || list.size() == 0)
            throw new IllegalArgumentException("not found any object to be serialized");
        Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(list.get(0).getClass());
        LinkedBuffer buffer = LinkedBuffer.allocate(1024 * 1024); // TODO 固定分配大小是否合适
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ProtostuffIOUtil.writeListTo(bos, list, schema, buffer);
            return bos.toByteArray();
        } catch (Exception e) {
            logger.error("[Serializer] serialize list error:" + e.getMessage());
            throw new RuntimeException("serialize list exception:" + e.getMessage());
        } finally {
            buffer.clear();
            try {
                bos.close();
            } catch (IOException e) {
                logger.debug("[Serializer] serialize list: close output stream error:" + e.getMessage());
            }
        }
    }

    public static <T> T deserialize(byte[] bytes, Class<T> targetClazz) {
        if (null == bytes || bytes.length == 0) throw new IllegalArgumentException("byte[] must not be null");
        if (null == targetClazz) throw new IllegalArgumentException("target class must not be null");
        Schema<T> schema = RuntimeSchema.getSchema(targetClazz);
        T instance;
        try {
            instance = targetClazz.newInstance();
        } catch (ReflectiveOperationException e) {
            logger.error("[Serializer] deserialize error:" + e.getMessage());
            throw new RuntimeException("deserialize exception:" + e.getMessage());
        }
        ProtostuffIOUtil.mergeFrom(bytes, instance, schema);
        return instance;
    }

    public static <T> List<T> deserializeList(byte[] bytes, Class<T> targetClazz) {
        if (null == bytes || bytes.length == 0) throw new IllegalArgumentException("byte[] must not be null");
        if (null == targetClazz) throw new IllegalArgumentException("target class must not be null");
        Schema<T> schema = RuntimeSchema.getSchema(targetClazz);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            return ProtostuffIOUtil.parseListFrom(bis, schema);
        } catch (Exception e) {
            logger.error("[Serializer] deserialize list error:" + e.getMessage());
            throw new RuntimeException("deserialize list exception:" + e.getMessage());
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                logger.debug("[Serializer] deserialize list: close input stream error:" + e.getMessage());
            }
        }
    }

}
