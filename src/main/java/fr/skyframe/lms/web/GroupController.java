package fr.skyframe.lms.web;

import fr.skyframe.lms.service.GroupService;
import fr.skyframe.lms.web.dto.GroupMemberRequest;
import fr.skyframe.lms.web.dto.GroupRequest;
import fr.skyframe.lms.web.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Student groups for group work. */
@RestController
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/courses/{courseId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@PathVariable Long courseId, @Valid @RequestBody GroupRequest request) {
        return GroupResponse.from(groupService.create(courseId, request.name()));
    }

    @GetMapping("/courses/{courseId}/groups")
    public List<GroupResponse> list(@PathVariable Long courseId) {
        return groupService.listForCourse(courseId).stream().map(GroupResponse::from).toList();
    }

    @GetMapping("/groups/{id}")
    public GroupResponse get(@PathVariable Long id) {
        return GroupResponse.from(groupService.get(id));
    }

    @PostMapping("/groups/{id}/members")
    public GroupResponse addMember(@PathVariable Long id, @Valid @RequestBody GroupMemberRequest request) {
        return GroupResponse.from(groupService.addMember(id, request.email()));
    }

    @DeleteMapping("/groups/{id}/members/{userId}")
    public GroupResponse removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return GroupResponse.from(groupService.removeMember(id, userId));
    }
}
