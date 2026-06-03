package com.viettel.demo.messaging;

/*
 * ==============================================================
 * MasterDataEventPublisher — boundary cho async messaging
 * ==============================================================
 *
 * Service nghiệp vụ chỉ biết publish event nghiệp vụ.
 * Chi tiết KafkaTemplate/topic/key nằm ở implementation phía dưới tầng
 * messaging, không đặt trong Controller hoặc trộn vào business code.
 *
 * ==============================================================
 */
public interface MasterDataEventPublisher {

    void publish(MasterDataChangedEvent event);
}
