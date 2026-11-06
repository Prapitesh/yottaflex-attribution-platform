package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name="entity_events",indexes=@Index(name="idx_entity_events_entity",columnList="entity_type,entity_id,occurred_at"))
public class EntityEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="entity_type",nullable=false,length=40) private String entityType;
    @Column(name="entity_id",nullable=false,length=110) private String entityId;
    @Column(name="event_type",nullable=false,length=40) private String eventType;
    @Column(name="old_state",length=30) private String oldState;
    @Column(name="new_state",length=30) private String newState;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,Object> metadata=new LinkedHashMap<>();
    protected EntityEvent(){}
    public EntityEvent(String type,String entityId,String eventType,String oldState,
            String newState,Instant at,String metadata){
        this(type,entityId,eventType,oldState,newState,at,Map.of());
    }
    public EntityEvent(String type,String entityId,String eventType,String oldState,
            String newState,Instant at,Map<String,Object> metadata){
        this.entityType=type;this.entityId=entityId;this.eventType=eventType;
        this.oldState=oldState;this.newState=newState;this.occurredAt=at;
        this.metadata=metadata==null?Map.of():Map.copyOf(metadata);
    }
}
