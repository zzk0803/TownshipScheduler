package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = GlobalResource.class, name = "global"),
                @JsonSubTypes.Type(value = LocalResource.class, name = "local"),
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(scope = Resource.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class Resource {

    private String id;

    private int capacity;

    protected Resource() {
    }

    protected Resource(String id, int capacity) {
        this(id);
        this.capacity = capacity;
    }

    protected Resource(String id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public abstract boolean isRenewable();

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Resource resource))
            return false;
        return Objects.equals(getId(), resource.getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
