package io.hostilerobot.ceramicrelief.drivers.rtee;

import javafx.event.Event;
import javafx.event.EventType;

public class TriangleChangedEvent extends Event {
    public static final EventType<TriangleChangedEvent> TRIANGLE_CHANGED = new EventType<>(EventType.ROOT, "TRIANGLE_CHANGED_EVENT");

    /**
     * since we're not actually utilizing the specifics of this event to recalculate the RTree,
     * we're just going to make this an empty event for now.
     *
     * However if it comes to it, we should have an event that represents reloading the triangles from the file,
     * and an event if a vertex is dragged.
     */
    public TriangleChangedEvent() {
        super(TRIANGLE_CHANGED);
    }
}
