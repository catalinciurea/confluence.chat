package confluence.chat.actions;

import com.atlassian.confluence.core.Beanable;
import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.opensymphony.webwork.ServletActionContext;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractChatAction extends ConfluenceActionSupport implements Beanable {

    private static String PARAM_MESSAGE = "message";
    private static String PARAM_CLOSE = "close";
    private static String PARAM_TO = "to";
    private ChatBoxMap chatBoxMap = new ChatBoxMap();
    private ChatManager chatManager;

    public AbstractChatAction(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    /**
     * Initial muss ich alle checkboxen holen, und mir dann anschauen, ob diese
     * nachrichten haben, die ich brauche (getInitMessagesShowSince)
     *
     * @return
     * @throws Exception
     */
    public final String start() throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        if (getRemoteUser() != null) {
            chatManager.setOnlineStatus(getRemoteUser(), ChatStatus.NO_CHANGE);
            ChatUser chatUser = chatManager.getChatUser(getRemoteUser());
            chatUser.setCurrentSite(request.getParameter("currentUrl"), request.getParameter("currentTitle"));
            ChatBoxMap chatBoxes = chatManager.getChatBoxes(getRemoteUser());

            Iterator<String> iterator = chatBoxes.keySet().iterator();
            while (iterator.hasNext()) {
                String chatBoxId = iterator.next();
                ChatBox chatBox = chatBoxes.get(chatBoxId);
                Date initMessagesShowSince = chatBox.getInitMessagesShowSince(session);
                this.addMessagesSince(chatBox, initMessagesShowSince);
            }
        }
        return SUCCESS;
    }

    /**
     * Add all important messages from given box
     *
     * @param chatBox
     * @param date
     */
    private void addMessagesSince(ChatBox chatBox, Date date) {
        if (chatBox.hasMessageSince(date)) {
            chatBoxMap.getChatBoxById(chatBox.getId()).setLastMessage(chatBox.getLastMessage());
            if (chatBox.isOpen()) {
                chatBoxMap.getChatBoxById(chatBox.getId()).open();
            } else {
                chatBoxMap.getChatBoxById(chatBox.getId()).close();
            }

            for (int j = chatBox.getMessages().size() - 1; j >= 0; j--) {
                ChatMessage message = chatBox.getMessages().get(j);
                if (message.getSenddate().after(date)) {
                    chatBoxMap.getChatBoxById(chatBox.getId()).addMessage(message);
                } else {
                    break;
                }
            }
            Collections.reverse(chatBoxMap.getChatBoxById(chatBox.getId()).getMessages());
        }
    }

    public final String close() throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        if (getRemoteUser() != null) {
            String parameter = request.getParameter(PARAM_CLOSE);
            if (StringUtils.isNotBlank(parameter)) {
                chatManager.closeChatBox(getRemoteUser(), new ChatBoxId(parameter));
            }
        }
        return SUCCESS;
    }

    public final String heartbeat() throws Exception {
        if (getRemoteUser() != null) {
            ChatUser chatUser = chatManager.getChatUser(getRemoteUser());
            // Keine senden, falls user offline
            if (!ChatStatus.OFFLINE.equals(chatUser.getStatus())) {
                chatManager.setOnlineStatus(getRemoteUser(), ChatStatus.NO_CHANGE);
                ChatBoxMap chatBoxes = chatManager.getChatBoxes(getRemoteUser());
                Date lastRequestDate = getLastRequestDate();
                Iterator<String> iterator = chatBoxes.keySet().iterator();
                while (iterator.hasNext()) {
                    ChatBox chatBox = chatBoxes.get(iterator.next());
                    this.addMessagesSince(chatBox, lastRequestDate);
                }
            }
        }
        return SUCCESS;
    }

    public final String send() throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        String message = request.getParameter(AbstractChatAction.PARAM_MESSAGE);
        String to = request.getParameter(AbstractChatAction.PARAM_TO);
        if (StringUtils.isNotEmpty(to)) {
            chatManager.sendMessage(getRemoteUser().getName(), to, message);
        }
//        return this.heartbeat();
        return SUCCESS;
    }

    @Override
    public Object getBean() {
        Date requestDate = new Date();
        Map<String, Object> bean = new HashMap<String, Object>();
        if (getRemoteUser() != null) {
            bean.put("lr", requestDate.getTime());

            if (!getChatBoxMap().isEmpty()) {
                List<Map> chatboxes = new ArrayList<Map>();
                Iterator<String> iterator = getChatBoxMap().keySet().iterator();
                while (iterator.hasNext()) {
                    chatboxes.add(getChatBoxMap().get(iterator.next()).getJSONMap(chatManager));
                }
                bean.put("chatboxes", chatboxes);
            }
        }
        return bean;
    }

    /**
     * @return the chatBoxMap
     */
    public ChatBoxMap getChatBoxMap() {
        if (chatBoxMap == null) {
            chatBoxMap = new ChatBoxMap();
        }
        return chatBoxMap;
    }

    private Date getLastRequestDate() {
        HttpServletRequest request = ServletActionContext.getRequest();
        String lr = request.getParameter("lr");
        Date date = null;
        if (StringUtils.isNumeric(lr)) {
            try {
                date = new Date(new Long(lr));
            } catch (Exception e) {
            }
        }
        if (date == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            date = cal.getTime();
        }
        return date;
    }
}
