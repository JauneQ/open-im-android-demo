package io.openim.android.ouicore.vm;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openim.android.ouicore.base.BaseViewModel;
import io.openim.android.ouicore.utils.Constant;
import io.openim.android.ouicore.utils.L;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.listener.OnBase;
import io.openim.android.sdk.models.FriendInfo;
import io.openim.android.sdk.models.FriendshipInfo;
import io.openim.android.sdk.models.GroupInfo;
import io.openim.android.sdk.models.GroupMembersInfo;
import io.openim.android.sdk.models.Message;
import io.openim.android.sdk.models.SearchResult;
import io.openim.android.sdk.models.SearchResultItem;
import io.openim.android.sdk.models.UserInfo;

public class SearchVM extends BaseViewModel {

    public MutableLiveData<List<SearchResultItem>> messageItems = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<SearchResultItem>> fileItems = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<GroupInfo>> groupsInfo = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<UserInfo>> userInfo = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<FriendInfo>> friendInfo = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<FriendshipInfo>> friendshipInfo = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<GroupMembersInfo>> groupMembersInfo = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<String> hail = new MutableLiveData<>();
    public MutableLiveData<String> remark = new MutableLiveData<>();
    //用户 或群组id
    public MutableLiveData<String> searchContent = new MutableLiveData<>("");

    //true 搜索人 false 搜索群
    public boolean isPerson = false;
    public int page;
    public int pageSize;

    public void searchPerson() {
        searchPerson(null);
    }

    public void searchPerson(List<String> ids) {
        if (null == ids) {
            ids = new ArrayList<>(); // 用户ID集合
            ids.add(searchContent.getValue());
        }
        //兼容旧版
        OpenIMClient.getInstance().userInfoManager.getUsersInfo(new OnBase<List<UserInfo>>() {
            @Override
            public void onError(int code, String error) {
                L.e(error + "-" + code);
                userInfo.setValue(null);
            }

            @Override
            public void onSuccess(List<UserInfo> data) {
                if (data.isEmpty()) return;
                userInfo.setValue(data);
            }
        }, ids);
    }

    public void checkFriend(List<UserInfo> data) {
        List<String> uIds = new ArrayList<>();
        uIds.add(data.get(0).getUserID());
        OpenIMClient.getInstance().friendshipManager.checkFriend(new OnBase<List<FriendshipInfo>>() {
            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onSuccess(List<FriendshipInfo> data) {
                friendshipInfo.setValue(data);
            }
        }, uIds);

    }

    public void searchGroupMemberByNickname(String groupId, String key) {
        List<String> keys = new ArrayList<>(); // 用户ID集合
        keys.add(key);
        OpenIMClient.getInstance().groupManager.searchGroupMembers(new OnBase<List<GroupMembersInfo>>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error+code);
            }

            @Override
            public void onSuccess(List<GroupMembersInfo> data) {
                if (key.isEmpty()) return;
                groupMembersInfo.getValue().addAll(data);
                groupMembersInfo.setValue(groupMembersInfo.getValue());
            }
        }, groupId, keys, false, true, page, pageSize);
    }

    public void addFriend() {
        OnBase<String> callBack = new OnBase<String>() {
            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onSuccess(String data) {
                Toast.makeText(getContext(), "发送成功", Toast.LENGTH_SHORT).show();
                getIView().onSuccess("");
//                String remarkStr = remark.getValue();
//                if (!TextUtils.isEmpty(remarkStr)) {
//                    OpenIMClient.getInstance().friendshipManager.setFriendRemark(null,
//                        searchContent.getValue(), remarkStr);
//                }
            }
        };
        if (isPerson)
            OpenIMClient.getInstance().friendshipManager.addFriend(callBack, searchContent.getValue(), hail.getValue());
        else
            OpenIMClient.getInstance().groupManager.joinGroup(callBack, searchContent.getValue(), hail.getValue(), 2);
    }

    public void search() {
        if (isPerson)
            searchPerson();
        else
            searchGroup(searchContent.getValue());
    }

    public void searchGroup(String gid) {
        List<String> groupIds = new ArrayList<>(); // 群ID集合
        groupIds.add(gid);
        OpenIMClient.getInstance().groupManager.getGroupsInfo(new OnBase<List<GroupInfo>>() {
            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onSuccess(List<GroupInfo> data) {
                groupsInfo.setValue(data);
            }
        }, groupIds);
    }

    public void searchFriendV2() {
        OpenIMClient.getInstance().friendshipManager.searchFriends(new OnBase<List<FriendInfo>>() {
            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onSuccess(List<FriendInfo> data) {
                if (page == 1) {
                    friendInfo.getValue().clear();
                }
                if (!data.isEmpty()) {
                    friendInfo.getValue().addAll(data);
                }
                friendInfo.setValue(friendInfo.getValue());
            }
        }, buildKeyWord(), false, true, true);
    }

    public void searchGroupV2() {
        OpenIMClient.getInstance().groupManager.searchGroups(new OnBase<List<GroupInfo>>() {
            @Override
            public void onError(int code, String error) {
            }

            @Override
            public void onSuccess(List<GroupInfo> data) {
                if (page == 1) {
                    groupsInfo.getValue().clear();
                }
                if (!data.isEmpty()) {
                    groupsInfo.getValue().addAll(data);
                }
                groupsInfo.setValue(groupsInfo.getValue());
            }
        }, buildKeyWord(), false, true);
    }

    public void searchLocalMessages(String key, Integer... messageTypes) {
        List<String> keys = null;
        if (!TextUtils.isEmpty(key)) {
            keys = new ArrayList<>();
            keys.add(key);
        }
        List<Integer> messageTypeLists;
        if (0 == messageTypes.length) {
            messageTypeLists = new ArrayList<>();
            messageTypeLists.add(Constant.MsgType.TXT);
            messageTypeLists.add(Constant.MsgType.MENTION);
        } else {
            messageTypeLists = Arrays.asList(messageTypes);
        }
        MutableLiveData<List<SearchResultItem>> items;
        List<Integer> type;
        if ((type = Arrays.asList(messageTypes)).size()
            == 1 && type.get(0) == Constant.MsgType.FILE) {
            items = fileItems;
        } else {
            items = messageItems;
        }
        OpenIMClient.getInstance()
            .messageManager
            .searchLocalMessages
                (new OnBase<SearchResult>() {
                     @Override
                     public void onError(int code, String error) {
                         getIView().toast(error + code);
                     }

                     @Override
                     public void onSuccess(SearchResult data) {
                         if (page == 1) {
                             items.getValue().clear();
                         }
                         if (data.getTotalCount() != 0) {
                             items.getValue().addAll(data.getSearchResultItems());
                         }
                         items.setValue(items.getValue());
                     }
                 }, null,
                    keys, 0,
                    null, messageTypeLists, 0,
                    0, page, pageSize);
    }

    private List<String> buildKeyWord() {
        List<String> keyWords = new ArrayList<>();
        keyWords.add(searchContent.getValue());
        return keyWords;
    }



    public void clearData() {
        messageItems.getValue().clear();
        fileItems.getValue().clear();
        groupsInfo.getValue().clear();
        userInfo.getValue().clear();
        friendInfo.getValue().clear();
    }
}
