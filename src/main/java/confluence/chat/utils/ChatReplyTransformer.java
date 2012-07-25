/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package confluence.chat.utils;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.user.User;
import confluence.chat.actions.ChatUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Dev
 */
public class ChatReplyTransformer {

    private PageManager pageManager;
    private PermissionManager permissionManager;

    public ChatReplyTransformer(PageManager pageManager, PermissionManager permissionManager) {
        this.pageManager = pageManager;
        this.permissionManager = permissionManager;
    }

    public List<Map> chatUserListToMap(User user, List<ChatUser> chatusers) {
        List<Map> list = new ArrayList<Map>();
        for (int i = 0; i < chatusers.size(); i++) {
            Map<String, String> userMap = new HashMap<String, String>();
            Map<String, String> jsonMap = chatusers.get(i).getJSONMap();

            for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
                if (ChatUser.CURRENT_CONTENT_ID.equals(entry.getKey())) {
                    if (StringUtils.isNumeric(entry.getValue())) {
                        ContentEntityObject cO = pageManager.getById(new Long(entry.getValue()));
                        if (cO != null) {
                            if (permissionManager.hasPermission(user, Permission.VIEW, cO)) {
                                userMap.put(ChatUser.CURRENT_SITE_TITLE, cO.getTitle());
                                userMap.put(ChatUser.CURRENT_SITE_URL, cO.getUrlPath());
                            }
                        }
                    }
                } else {
                    userMap.put(entry.getKey(), entry.getValue());
                }
            }
            list.add(userMap);
        }
        return list;
    }
}
