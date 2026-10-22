package com.example.starter.domain.port.out;

import com.example.starter.domain.model.event.DomainEvent;

public interface PublishEventPort {

    void publish(DomainEvent event);
}
