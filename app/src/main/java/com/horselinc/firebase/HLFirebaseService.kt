package com.horselinc.firebase

import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.horselinc.*
import com.horselinc.R
import com.horselinc.models.data.*
import com.horselinc.models.event.HLEventPaymentInvoiceUpdated
import com.horselinc.models.event.HLEventPaymentInvoicesUpdated
import com.horselinc.models.event.HLEventServiceRequestsCompleted
import com.horselinc.utils.ResourceUtil
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class HLFirebaseService {

    companion object {
        var instance = HLFirebaseService ()
    }

    /**
     *  Authorization
     */
    private val auth = FirebaseAuth.getInstance()
    val isAuthorized: Boolean
        get() {
            return auth.currentUser != null
        }

    /**
     *  Collections
     */
    private val db = FirebaseFirestore.getInstance()
    private val collectionUsers = db.collection("users")
    private val collectionSettings = db.collection("settings")
    private val collectionPaymentApprovers = db.collection("payment-approvers")
    private val collectionHorses = db.collection("horses")
    private val collectionHorseOwners = db.collection("horse-owners")
    private val collectionProviderServices = db.collection("service-provider-services")
    private val collectionNotifications = db.collection("notifications")
    private val collectionServiceRequests = db.collection("service-requests")
    private val collectionServiceShows = db.collection("service-shows")
    private val collectionInvoices = db.collection("invoices")
    private val collectionManagerServiceProviders = db.collection("horse-manager-providers")
    private val collectionPayments = db.collection("payments")

    /**
     *  Cloud Function
     */
    private val functions = FirebaseFunctions.getInstance()


    /**
     *  Storage
     */
    private val storage = FirebaseStorage.getInstance()
    private val storageUsers = storage.getReference("users")
    private val storageHorses = storage.getReference("horses")

    /**
     * Listener
     */
    private var listenerManagerServiceRequest: ListenerRegistration? = null
    private var listenerProviderServiceRequest: ListenerRegistration? = null
    private var listenerServiceRequestsCompleted: ListenerRegistration? = null
    private var listenerPaymentStatusUpdated: ListenerRegistration? = null
    private var listenerNewNotification: ListenerRegistration? = null
    private var listenerInvoiceUpdate: ListenerRegistration? = null

    /**
     *  Others
     */
    private val successMsg = ResourceUtil.getString(R.string.msg_sucess)

    /**
     *  Create Horse Owner
     */
    fun createHorseOwner (email: String, password: String, callback: ResponseCallback<String>) {
        val params = hashMapOf<String, Any>(
            "email" to email,
            "password" to password
        )

        functions.getHttpsCallable("createHorseOwner")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    (task.result as? HashMap<*, *>)?.let {
                        val uid = it["uid"] as? String
                        if (uid != null && uid.isNotEmpty()) {
                            callback.onSuccess(uid)
                        } else {
                            callback.onFailure("Cannot get horse owner data (null)")
                        }

                    } ?: callback.onFailure("Cannot get horse owner data")
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot create horse owner"
                    )
                }
            }
    }

    /**
     *  Authorization Handlers
     */
    fun signUp (email: String, password: String, callback: ResponseCallback<HLUserModel>) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user == null) {
                        callback.onFailure(task.exception?.localizedMessage ?: "Cannot get created user information")
                    } else {
                        val newUser = HLUserModel().apply {
                            this.uid = user.uid
                            this.email = email
                            this.status = HLUserOnlineStatus.ONLINE
                            this.platform = HLPlatformType.ANDROID
                            this.createdAt = Calendar.getInstance().timeInMillis

                            HLGlobalData.token?.let {
                                this.token = it
                            }
                        }

                        collectionUsers.document(user.uid)
                            .set(newUser, SetOptions.merge())
                            .addOnSuccessListener {
                                getUser(user.uid, callback)
                            }
                            .addOnFailureListener { ex ->
                                callback.onFailure(ex.localizedMessage ?: "Cannot set new user")
                            }
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot sign up with this information")
                }
            }
    }

    fun login (email: String, password: String, callback: ResponseCallback<HLUserModel>) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {  task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user == null) {
                        callback.onFailure(task.exception?.localizedMessage ?: "Cannot get login user information")
                    } else {
                        val newValues = hashMapOf<String, Any>(
                            "status" to HLUserOnlineStatus.ONLINE,
                            "platform" to HLPlatformType.ANDROID)

                        HLGlobalData.token?.let {
                            newValues["token"] = it
                        }

                        collectionUsers.document(user.uid)
                            .update(newValues)
                            .addOnSuccessListener {
                                getUser(user.uid, callback)
                            }
                            .addOnFailureListener { ex ->
                                callback.onFailure(ex.localizedMessage ?: "Cannot update user information")
                            }
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot login with this user")
                }
            }
    }

    fun forgotPassword (email: String, callback: ResponseCallback<String>) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess("Sent an reset password instruction to your email address")
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot reset password")
                }
            }
    }

    fun changePassword (password: String, callback: ResponseCallback<String>) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.updatePassword(password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.onSuccess(successMsg)
                    } else {
                        callback.onFailure(task.exception?.localizedMessage ?: "Cannot change your password")
                    }
                }
        } else {
            callback.onFailure("Cannot change your password (null)")
        }
    }

    fun logout (callback: ResponseCallback<String>) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            collectionUsers.document(currentUser.uid)
                .update(mapOf("status" to HLUserOnlineStatus.OFFLINE,
                    "token" to FieldValue.delete()))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.signOut()
                        callback.onSuccess(successMsg)
                    } else {
                        callback.onFailure(task.exception?.localizedMessage ?: "Cannot logout")
                    }
                }

        } else {
            callback.onFailure("Cannot logout (null)!")
        }
    }

    fun registerToken (token: String, callback: ResponseCallback<String>) {
        auth.currentUser?.let { user ->
            collectionUsers.document(user.uid)
                .update(mapOf("token" to token))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.onSuccess(successMsg)
                    } else {
                        callback.onFailure(task.exception?.localizedMessage ?: "Cannot update user token!")
                    }
                }
        }
    }

    /**
     *  Settings Handler
     */
    fun getSettings (callback: ResponseCallback<HLSettingsModel>) {
        collectionSettings.get()
            .addOnSuccessListener {
                val settings = it.documents.first().toObject(HLSettingsModel::class.java)
                if (settings == null) {
                    callback.onFailure("Cannot get settings")
                } else {
                    callback.onSuccess(settings)
                }
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get settings (null)")
            }
    }

    /**
     *  User Handlers
     */
    fun getUser (userId: String, callback: ResponseCallback<HLUserModel>) {
        collectionUsers.document(userId).get()
            .addOnSuccessListener { docSnapshot ->
                val user = docSnapshot.toObject(HLUserModel::class.java)
                if (user == null) {
                    callback.onFailure("Cannot convert user information")
                } else {
                    user.uid = userId
                    callback.onSuccess(user)
                }
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get user information")
            }
    }

    fun uploadAvatar (userId: String, userType: String, avatarUri: Uri, callback: ResponseCallback<String>) {
        val metaData = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        val fileName = if (userType == HLUserType.MANAGER) "manager.jpg" else "provider.jpg"
        val storageRef = storageUsers.child("$userId/$fileName")

        storageRef.putFile(avatarUri, metaData)
            .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot upload avatar")
                }
                return@Continuation storageRef.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    if (downloadUri == null) {
                        callback.onFailure("Cannot get uploaded avatar url")
                    } else {
                        callback.onSuccess(downloadUri.toString())
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot upload avatar")
                }
            }
    }

    fun updateUser (user: HLUserModel, shouldUpdateEmail: Boolean = false, callback: ResponseCallback<String>) {
        if (shouldUpdateEmail) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.updateEmail(user.email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            collectionUsers.document(user.uid)
                                .set(user, SetOptions.merge())
                                .addOnSuccessListener {
                                    callback.onSuccess(successMsg)
                                }
                                .addOnFailureListener { ex ->
                                    callback.onFailure(ex.localizedMessage ?: "Cannot update user (null)")
                                }
                        } else {
                            when(task.exception) {
                                // we need to ask use to relogin
                                is FirebaseAuthRecentLoginRequiredException -> callback.onFailure("FirebaseAuthRecentLoginRequiredException")
                                else -> callback.onFailure(task.exception?.localizedMessage ?: "Cannot change your password")
                            }
                        }
                    }
            } else {
                callback.onFailure("Cannot change user email (null)")
            }
        } else {
            collectionUsers.document(user.uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener {
                    callback.onSuccess(successMsg)
                }
                .addOnFailureListener { ex ->
                    callback.onFailure(ex.localizedMessage ?: "Cannot update user (null)")
                }
        }
    }

    fun createUser (user: HLUserModel, callback: ResponseCallback<String>) {
        collectionUsers.document(user.uid)
            .set(user, SetOptions.merge())
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot create user (null)")
            }
    }

    fun reauthenticate(email: String, password: String, callback: ResponseCallback<Boolean>) {
        val credential = EmailAuthProvider.getCredential(email, password)
        auth.currentUser?.apply {
            if (email != this.email) {
                callback.onFailure("Invalid email address")
                return@apply
            }

            reauthenticate(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        callback.onSuccess(true)
                    } else {
                        callback.onFailure("Invalid password")
                    }
                }
                .addOnFailureListener {
                    callback.onFailure("Invalid password")
                }
        } ?: callback.onFailure("Can't update email")
    }

    fun reauthenticate(password: String, callback: ResponseCallback<Boolean>) {
        auth.currentUser?.apply {
            val credential = EmailAuthProvider.getCredential(email.orEmpty(), password)
            reauthenticate(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        callback.onSuccess(true)
                    else
                        callback.onFailure("Invalid password")
                }
                .addOnFailureListener {
                    callback.onFailure(it.localizedMessage ?: "Invalid password")
                }
        } ?: callback.onFailure("Can't update password")
    }

    fun getPaymentApprovers (userId: String, callback: ResponseCallback<List<HLHorseManagerPaymentApproverModel>>) {
        collectionPaymentApprovers.whereEqualTo("creatorId", userId)
            .get()
            .addOnSuccessListener { querySnapshots ->
                val approvers = ArrayList<HLHorseManagerPaymentApproverModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLHorseManagerPaymentApproverModel::class.java)?.let {
                        approvers.add(it)
                    }
                }
                callback.onSuccess(approvers)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get approvers (null)")
            }
    }

    fun addApprover (approver: HLHorseManagerPaymentApproverModel, callback: ResponseCallback<String>) {
        val ref = collectionPaymentApprovers.document()
        approver.uid = ref.id
        ref.set(approver)
            .addOnSuccessListener {
                callback.onSuccess(ref.id)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot add approver (null)")
            }
    }

    fun updateApprover (approver: HLHorseManagerPaymentApproverModel, callback: ResponseCallback<String>) {
        collectionPaymentApprovers.document(approver.uid)
            .set(approver)
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot update approver (null)")
            }
    }

    fun deleteApprover (approverUid: String, callback: ResponseCallback<String>) {
        collectionPaymentApprovers.document(approverUid)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete approver (null)")
            }
    }

    fun getProviderServices (userId: String, callback: ResponseCallback<List<HLServiceProviderServiceModel>>) {
        collectionProviderServices.whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshots ->
                val services = ArrayList<HLServiceProviderServiceModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLServiceProviderServiceModel::class.java)?.let {
                        services.add(it)
                    }
                }
                callback.onSuccess(services)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get provider services (null)")
            }
    }

    fun addProviderServices (services: List<HLServiceProviderServiceModel>, callback: ResponseCallback<String>) {
        val batch = db.batch()
        services.forEach { service ->
            val ref = collectionProviderServices.document()
            service.uid = ref.id
            batch.set(ref, service, SetOptions.merge())
        }
        batch.commit()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot add provider services (null)")
            }
    }

    fun deleteProviderService (serviceUid: String, callback: ResponseCallback<String>) {
        collectionProviderServices.document(serviceUid)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete provider service (null)")
            }
    }

    /**
     *  Horse Handlers
     */
    fun addHorse (horse: HLHorseModel, avatarUri: Uri?, callback: ResponseCallback<String>) {
        val ref = collectionHorses.document()
        horse.uid = ref.id
        if (avatarUri != null) {
            uploadHorseAvatar(horse, avatarUri, null, callback)
        } else {
            setHorseData(horse, null, callback)
        }
    }

    fun updateHorse (horse: HLHorseModel, avatarUri: Uri?, updateOwnerIds: ArrayList<String>, callback: ResponseCallback<String>) {
        if (avatarUri != null) {
            uploadHorseAvatar(horse, avatarUri, updateOwnerIds, callback)
        } else {
            setHorseData(horse, updateOwnerIds, callback)
        }
    }

    fun updateHorse (horseId: String, updateData: HashMap<String, Any>, callback: ResponseCallback<String>) {
        collectionHorses.document(horseId)
            .update(updateData)
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot update horse (null)")
            }
    }

    fun deleteHorse (horseId: String, callback: ResponseCallback<String>) {
        collectionHorses.document(horseId)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete horse (null)")
            }
    }

    private fun uploadHorseAvatar (horse: HLHorseModel,
                                   avatarUri: Uri,
                                   updateOwnerIds: ArrayList<String>?,
                                   callback: ResponseCallback<String>) {
        val metaData = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        val storageRef = storageHorses.child("${horse.creatorId}/${horse.uid}")

        storageRef.putFile(avatarUri, metaData)
            .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot upload horse avatar")
                }
                return@Continuation storageRef.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    if (downloadUri == null) {
                        callback.onFailure("Cannot get uploaded horse avatar url")
                    } else {
                        horse.avatarUrl = downloadUri.toString()
                        setHorseData(horse, updateOwnerIds, callback)
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot upload horse avatar (null)")
                }
            }
    }

    private fun setHorseData (horse: HLHorseModel, updateOwnerIds: ArrayList<String>?, callback: ResponseCallback<String>) {
        if (updateOwnerIds != null) { // update exist
            collectionHorses.document(horse.uid)
                .set(horse, SetOptions.merge())
                .addOnSuccessListener {
                    updateHorseOwners (horse, updateOwnerIds, callback)
                }
                .addOnFailureListener { ex ->
                    callback.onFailure(ex.localizedMessage ?: "Cannot update horse (null)")
                }
        } else { // add new
            collectionHorses.document(horse.uid)
                .set(horse, SetOptions.merge())
                .addOnSuccessListener {
                    addHorseOwners (horse.uid, horse.owners, callback)
                }
                .addOnFailureListener { ex ->
                    callback.onFailure(ex.localizedMessage ?: "Cannot add horse (null)")
                }
        }
    }

    private fun addHorseOwners (horseId: String, owners: List<HLHorseOwnerModel>?, callback: ResponseCallback<String>) {
        if (owners == null) {
            callback.onSuccess(horseId)
        } else {
            val batch = db.batch()
            owners.forEach { owner ->
                val ref = collectionHorseOwners.document()
                owner.uid = ref.id
                owner.horseId = horseId
                batch.set(ref, owner, SetOptions.merge())
            }
            batch.commit()
                .addOnSuccessListener {
                    callback.onSuccess(horseId)
                }
                .addOnFailureListener { ex ->
                    callback.onFailure(ex.localizedMessage ?: "Cannot add horse owners (null)")
                }
        }
    }

    private fun updateHorseOwners (horse: HLHorseModel, updateOwnerIds: ArrayList<String>?, callback: ResponseCallback<String>) {
        if (horse.owners != null) {
            val batch = db.batch()

            // add new owners
            val newOwners = horse.owners?.filter { owner -> owner.uid.isEmpty() }
            newOwners?.forEach {newOwner ->
                val ref = collectionHorseOwners.document()
                newOwner.uid = ref.id
                newOwner.horseId = horse.uid
                batch.set(ref, newOwner, SetOptions.merge())
            }

            // update exist owners
            updateOwnerIds?.let { ownerUids ->
                val existOwnerIds = horse.owners?.filter { owner -> owner.uid.isNotEmpty() }?.map { it.uid }
                val updateIds = ownerUids.filter { ownerUid -> existOwnerIds?.contains(ownerUid) == true }
                val deleteIds = ownerUids.filter { ownerUid -> existOwnerIds?.contains(ownerUid) == false }

                // update owners
                val existOwners = horse.owners?.filter { owner -> owner.uid.isNotEmpty() }
                updateIds.forEach { updateId ->
                    val updateOwner = existOwners?.first { owner -> owner.uid == updateId }
                    updateOwner?.let { owner ->
                        batch.update(collectionHorseOwners.document(owner.uid), hashMapOf<String, Any>("percentage" to owner.percentage))
                    }
                }

                // delete owners
                deleteIds.forEach {  deleteId ->
                    batch.delete(collectionHorseOwners.document(deleteId))
                }
            }

            batch.commit()
                .addOnSuccessListener {
                    callback.onSuccess(horse.uid)
                }
                .addOnFailureListener { ex ->
                    callback.onFailure(ex.localizedMessage ?: "Cannot update horse owners (null)")
                }
        } else {
            callback.onSuccess(horse.uid)
        }
    }

    /**
     * Notifications
     */

    /**
     * Get the number of unread messages
     *
     * parameters:
     * @userId : horse manager user id or service provider user id.
     * @callback: callback listener
     *
     * @return: The number of notifications
     */
    fun getUnreadNotificationCount(userId: String, callback: ResponseCallback<Int>) {
        collectionNotifications
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { querySnapshots ->
                callback.onSuccess(querySnapshots.documents.count())
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get notifications (null)")
            }
    }

    /**
     * Subscribe new notification
     *
     * parameters:
     * @userId : horse manager user id or service provider user id.
     * @callback: callback listener
     *
     * @return: boolean value (true)
     */
    fun subscribeNewNotification(userId: String, callback: ResponseCallback<QuerySnapshot>) {
        listenerNewNotification = collectionNotifications
            .whereEqualTo("receiverId", userId)
            .whereGreaterThanOrEqualTo("updatedAt", Date().time)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { querySnapshot, ex ->
                if (ex != null) {
                    callback.onFailure(ex.localizedMessage ?: "Cannot subscribe notification (null)")
                } else if (querySnapshot != null) {
                    callback.onSuccess(querySnapshot)
                }
            }
    }

    fun getNotifications (userId: String, lastDocument: DocumentSnapshot?, callback: ResponseCallback<List<HLNotificationModel>>) {
        val ref = collectionNotifications.whereEqualTo("receiverId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        lastDocument?.let {
            ref.startAfter(it)
        }

        ref.limit(HLConstants.LIMIT_NOTIFICATIONS)
        ref.get()
            .addOnSuccessListener { querySnapshots ->
                val notifications = ArrayList<HLNotificationModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLNotificationModel::class.java)?.let {
                        it.uid = doc.id
                        it.documentSnapshot = doc
                        notifications.add(it)
                    }
                }
                callback.onSuccess(notifications)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get notifications (null)")
            }
    }

    fun deleteNotification (notificationId: String, callback: ResponseCallback<String>) {
        collectionNotifications.document(notificationId)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete notification (null)")
            }
    }

    fun removeNewNotificationListener () {
        listenerNewNotification?.remove()
        listenerNewNotification = null
    }

    /**
     *  Service Request Handler
     */
    fun deleteServiceRequest (requestId: String, callback: ResponseCallback<String>) {
        collectionServiceRequests.document(requestId)
            .update(hashMapOf(
                "status" to HLServiceRequestStatus.DELETED,
                "updatedAt" to Calendar.getInstance().timeInMillis))
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete service request (null)")
            }
    }

    fun addServiceRequestListenerForManager (userId: String, callback: ResponseCallback<List<HLServiceRequestModel>>) {
        val params = hashMapOf(
            "userId" to userId,
            "userType" to HLUserType.MANAGER
        )

        listenerManagerServiceRequest = collectionServiceRequests.whereGreaterThan("updatedAt", Calendar.getInstance().timeInMillis)
            .whereArrayContains("listenerUsers", params)
            .whereEqualTo("isCustomRequest", false)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, ex ->
                if (ex != null) {
                    callback.onFailure(ex.localizedMessage ?: "Cannot add service request listener")
                } else {
                    val requests = ArrayList<HLServiceRequestModel>()
                    querySnapshot?.documentChanges?.let { changes ->
                        changes.forEach { change ->
                            val request = change.document.toObject(HLServiceRequestModel::class.java).apply {
                                this.diffType = change.type
                            }
                            requests.add(request)
                        }
                    }

                    callback.onSuccess(requests)
                }
            }
    }

    fun addServiceRequestListenerForProvider (userId: String, callback: ResponseCallback<List<HLServiceRequestModel>>) {
        val params = hashMapOf(
            "userId" to userId,
            "userType" to HLUserType.PROVIDER
        )

        listenerProviderServiceRequest = collectionServiceRequests.whereGreaterThan("updatedAt", Calendar.getInstance().timeInMillis)
            .whereArrayContains("listenerUsers", params)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, ex ->
                if (ex != null) {
                    callback.onFailure(ex.localizedMessage ?: "Cannot add service request listener")
                } else {
                    val requests = ArrayList<HLServiceRequestModel>()
                    querySnapshot?.documentChanges?.let { changes ->
                        changes.forEach { change ->
                            val request = change.document.toObject(HLServiceRequestModel::class.java).apply {
                                this.diffType = change.type
                            }
                            requests.add(request)
                        }
                    }

                    callback.onSuccess(requests)
                }
            }
    }

    fun updateServiceRequest (requestId: String, updateData: HashMap<String, Any>, callback: ResponseCallback<String>) {
        collectionServiceRequests.document(requestId)
            .update(updateData)
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot update service request (null)")
            }
    }

    fun updateServiceRequest (request: HLServiceRequestModel, callback: ResponseCallback<String>) {
        collectionServiceRequests.document(request.uid)
            .set(request, SetOptions.merge())
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot update service request (null)")
            }
    }

    fun addServiceRequest (request: HLServiceRequestModel, callback: ResponseCallback<String>) {
        val ref = collectionServiceRequests.document()
        request.uid = ref.id

        // create new service request
        ref.set(request, SetOptions.merge())
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot add service request (null)")
            }
    }

    fun getServiceShows (callback: ResponseCallback<List<HLServiceShowModel>>) {
        collectionServiceShows.get()
            .addOnSuccessListener {  querySnapshots ->
                val shows = ArrayList<HLServiceShowModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLServiceShowModel::class.java)?.let {
                        shows.add(it)
                    }
                }
                callback.onSuccess(shows)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get service shows(null)")
            }
    }

    /**
     *  Function Handlers
     */
    fun createCustomer (userId: String, callback: ResponseCallback<HLStripeCustomerModel>) {
        val data = hashMapOf("userId" to userId)

        functions.getHttpsCallable("createCustomer")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customer = task.result?.toCustomObject(HLStripeCustomerModel::class.java)
                    if (customer == null) {
                        callback.onFailure("Cannot create customer")
                    } else {
                        callback.onSuccess(customer)
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot create customer (null)")
                }
            }
    }

    fun addCardToCustomer (userId: String, customerId: String, sourceId: String, callback: ResponseCallback<HLStripeCustomerModel>) {
        val data = hashMapOf(
            "userId" to userId,
            "customerId" to customerId,
            "sourceId" to sourceId
        )

        functions.getHttpsCallable("addCardToCustomer")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customer = task.result?.toCustomObject(HLStripeCustomerModel::class.java)
                    if (customer == null) {
                        callback.onFailure("Cannot add card")
                    } else {
                        callback.onSuccess(customer)
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot add card (null)")
                }
            }
    }

    fun changeDefaultCard (userId: String, customerId: String, cardId: String, callback: ResponseCallback<HLStripeCustomerModel>) {
        val data = hashMapOf(
            "userId" to userId,
            "customerId" to customerId,
            "sourceId" to cardId
        )

        functions.getHttpsCallable("changeDefaultCard")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customer = task.result?.toCustomObject(HLStripeCustomerModel::class.java)
                    if (customer == null) {
                        callback.onFailure("Cannot change card")
                    } else {
                        callback.onSuccess(customer)
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot change card (null)")
                }
            }
    }

    fun deleteCard (userId: String, customerId: String, cardId: String, callback: ResponseCallback<HLStripeCustomerModel>) {
        val data = hashMapOf(
            "userId" to userId,
            "customerId" to customerId,
            "sourceId" to cardId
        )

        functions.getHttpsCallable("deleteCard")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customer = task.result?.toCustomObject(HLStripeCustomerModel::class.java)
                    if (customer == null) {
                        callback.onFailure("Cannot delete card")
                    } else {
                        callback.onSuccess(customer)
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot delete card (null)")
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun getExpressLoginUrl (accountId: String, callback: ResponseCallback<String>) {

        val data = hashMapOf("accountId" to accountId)

        functions.getHttpsCallable("getExpressLoginUrl")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result as? Map<String, Any>
                    val url = result?.get("url")
                    if (url != null && url is String) {
                        callback.onSuccess(url)
                    } else {
                        callback.onFailure("Cannot get express login url")
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get express login url (null)")
                }
            }
    }

    fun searchHorseManagers (query: String, lastUserId: String?, excludeIds: ArrayList<String>?,
                             callback: ResponseCallback<List<HLHorseManagerModel>>) {
        val data = hashMapOf(
            "query" to query,
            "limit" to HLConstants.LIMIT_HORSE_MANAGERS
        )

        lastUserId?.let {
            data["lastUserId"] = it
        }

        excludeIds?.let {
            data["excludeIds"] = it
        }

        functions.getHttpsCallable("searchHorseManagers")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val managers = ArrayList<HLHorseManagerModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLHorseManagerModel::class.java)?.let { manager ->
                            managers.add(manager)
                        }
                    }
                    callback.onSuccess(managers)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get horses (null)")
                }
            }
    }

    fun searchServiceProviders (query: String, lastUserId: String?, excludeIds: ArrayList<String>?,
                                callback: ResponseCallback<List<HLServiceProviderModel>>) {
        val data = hashMapOf(
            "query" to query,
            "limit" to HLConstants.LIMIT_HORSE_MANAGERS
        )

        lastUserId?.let {
            data["lastUserId"] = it
        }

        excludeIds?.let {
            data["excludeIds"] = it
        }

        functions.getHttpsCallable("searchServiceProviders")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val providers = ArrayList<HLServiceProviderModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLServiceProviderModel::class.java)?.let { provider ->
                            providers.add(provider)
                        }
                    }
                    callback.onSuccess(providers)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get service providers (null)")
                }
            }
    }

    fun searchHorsesForManager (userId: String,
                                lastHorseId: String,
                                filter: HLHorseFilterModel?,
                                callback: ResponseCallback<List<HLHorseModel>>,
                                query: String? = null) {
        val data = hashMapOf(
            "userId" to userId,
            "limit" to HLConstants.LIMIT_HORSES
        )

        if (lastHorseId.isNotEmpty()) {
            data["lastHorseId"] = lastHorseId
        }

        filter?.trainer?.let {
            data["trainerId"] = it.userId
        }

        filter?.owner?.let {
            data["ownerId"] = it.userId
        }

        filter?.sortFieldName?.let {
            data["sort"] = mapOf(
                "name" to it,
                "order" to filter.sortOrder
            )
        }

        query?.let { data["query"] = it }

        functions.getHttpsCallable("searchHorses")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val horses = ArrayList<HLHorseModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLHorseModel::class.java)?.let { horse ->
                            horses.add(horse)
                        }
                    }
                    callback.onSuccess(horses)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get horses (null)")
                }
            }
    }

    fun searchServiceShows (query: String,
                            lastShowId: String?,
                            excludeIds: List<String>?,
                            callback: ResponseCallback<List<HLServiceShowModel>>) {

        val data = hashMapOf(
            "query" to query,
            "limit" to HLConstants.LIMIT_SERVICE_SHOWS
        )

        if (null != lastShowId && lastShowId.isNotEmpty()) {
            data["lastShowId"] = lastShowId
        }

        if (null != excludeIds && excludeIds.isNotEmpty()) {
            data["excludeIds"] = excludeIds
        }

        functions.getHttpsCallable("searchServiceShows")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val show = ArrayList<HLServiceShowModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLServiceShowModel::class.java)?.let { horse ->
                            show.add(horse)
                        }
                    }
                    callback.onSuccess(show)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get shows (null)")
                }
            }
    }

    fun searchHorsesForProvider (userId: String, filter: HLHorseFilterModel? = null, callback: ResponseCallback<List<HLProviderHorseModel>>) {
        val data = hashMapOf(
            "userId" to userId
        )

        filter?.let { f ->
            f.manager?.let {
                data["managerId"] = it.userId
            }

            f.sortFieldName?.let {
                data[it] = f.sortOrder
            }
        }

        functions.getHttpsCallable("searchHorses")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val horses = ArrayList<HLProviderHorseModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLProviderHorseModel::class.java)?.let { horse ->
                            horses.add(horse)
                        }
                    }
                    callback.onSuccess(horses)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get horses for provider (null)")
                }
            }
    }

    fun searchHorseUsers (userId: String,
                          searchType: String,
                          lastUserId: String,
                          excludeIds: ArrayList<String>? = null,
                          callback: ResponseCallback<List<HLHorseManagerModel>>) {
        val data = hashMapOf(
            "userId" to userId,
            "searchType" to searchType,
            "limit" to HLConstants.LIMIT_HORSE_USERS
        )

        if (lastUserId.isNotEmpty()) {
            data["lastUserId"] = lastUserId
        }

        excludeIds?.let {
            data["excludeIds"] = excludeIds
        }

        functions.getHttpsCallable("searchHorseUsers")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val users = ArrayList<HLHorseManagerModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLHorseManagerModel::class.java)?.let { user ->
                            users.add(user)
                        }
                    }
                    callback.onSuccess(users)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get horse users (null)")
                }
            }
    }

    fun searchServiceRequestsForManager (horseId: String,
                                         statuses: ArrayList<String>?,
                                         lastRequestId: String?,
                                         callback: ResponseCallback<List<HLServiceRequestModel>>) {
        val data = hashMapOf(
            "horseId" to horseId,
            "limit" to HLConstants.LIMIT_SERVICE_REQUESTS
        )

        lastRequestId?.let {
            data["lastRequestId"] = it
        }

        statuses?.let {
            if (it.isNotEmpty()) {
                data["statuses"] = it
            }
        }

        functions.getHttpsCallable("searchServiceRequests")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val requests = ArrayList<HLServiceRequestModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLServiceRequestModel::class.java)?.let { request ->
                            requests.add(request)
                        }
                    }
                    callback.onSuccess(requests)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get service requests (null)")
                }
            }
    }

    fun getServiceRequests (requestIds: List<String>, callback: ResponseCallback<List<HLServiceRequestModel>>) {
        functions.getHttpsCallable("getServiceRequests")
            .call(hashMapOf("serviceRequestIds" to requestIds))
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val requests = ArrayList<HLServiceRequestModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLServiceRequestModel::class.java)?.let { request ->
                            requests.add(request)
                        }
                    }
                    callback.onSuccess(requests)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get service requests (null)")
                }
            }
    }

    fun removeServiceRequestListeners () {
        listenerManagerServiceRequest?.remove()
        listenerProviderServiceRequest?.remove()

        listenerManagerServiceRequest = null
        listenerProviderServiceRequest = null
    }

    fun removeListeners() {
        listenerServiceRequestsCompleted?.remove()
        listenerPaymentStatusUpdated?.remove()

        listenerServiceRequestsCompleted = null
        listenerPaymentStatusUpdated = null

        removeServiceRequestListeners()
        removeInvoiceStatusUpdatedListener()
        removeNewNotificationListener()
    }


    fun putServiceProviderServices(services: List<HLServiceProviderServiceModel>, callback: ResponseCallback<List<HLServiceProviderServiceModel>>) {

        val batch = db.batch()

        services.forEach { service ->
            var uid = service.uid
            if (uid.isEmpty()) {
                uid = collectionProviderServices.document().id
                service.uid = uid
            }

            batch.set(collectionProviderServices.document(uid), service, SetOptions.merge())
        }

        batch.commit().addOnCompleteListener {
            if (it.isSuccessful) {
                callback.onSuccess(services)
            } else {
                callback.onFailure(it.exception?.localizedMessage ?: "Something went wrong")
            }
        }.addOnFailureListener {
            callback.onFailure(it.localizedMessage ?: "Something went wrong")
        }
    }

    fun putShow(show: HLServiceShowModel, callback: ResponseCallback<HLServiceShowModel>) {

        var uid = show.uid
        if (uid.isEmpty()) {
            uid = collectionServiceShows.document().id
            show.createdAt = Date().time
            show.uid = uid
        }

        collectionServiceShows.document(uid).set(show, SetOptions.merge())
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    callback.onSuccess(show)
                } else {
                    callback.onFailure(it.exception?.localizedMessage ?: "Something went wrong")
                }
            }.addOnFailureListener {
                callback.onFailure(it.localizedMessage ?: "Something went wrong")
            }
    }

    /**
    Create or edit a service request

    - parameters:
    - serviceRequest: HLServiceRequestModel. service request model to be saved
    - completion: ((HLServiceRequestModel?, String?) -> Void)?
    - the saved service request model: HLServiceRequestModel?
    - error string: String?
     */
    fun putServiceRequest(serviceRequest: HLServiceRequestModel, callback: ResponseCallback<HLServiceRequestModel>) {

        fun saveServiceRequest() {
            val cloned = serviceRequest.copy()
            var uid = cloned.uid
            if (uid.isEmpty()) {
                uid = collectionServiceRequests.document().id
                cloned.uid = uid
            }

            collectionServiceRequests.document(uid).set(cloned, SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        serviceRequest.uid = cloned.uid
                        callback.onSuccess(serviceRequest)
                    } else {
                        callback.onFailure(it.exception?.localizedMessage ?: "Something went wrong")
                    }
                }.addOnFailureListener {
                    callback.onFailure(it.localizedMessage ?: "Something went wrong")
                }
        }

        serviceRequest.updatedAt = Date().time

        var count = 0

        if (HLGlobalData.me?.type == HLUserType.PROVIDER && HLGlobalData.me?.serviceProvider?.userId == serviceRequest.serviceProviderId) {
            /*
            if (0 < serviceRequest.services.size) {
                count++
                val services = serviceRequest.services.map { it.copy() }
                putServiceProviderServices(
                    services,
                    object : ResponseCallback<List<HLServiceProviderServiceModel>> {
                        override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                            services.forEachIndexed { index, s ->
                                serviceRequest.services[index].uid = s.uid
                            }
                            count--

                            if (1 > count) saveServiceRequest()
                        }

                        override fun onFailure(error: String) {
                            callback.onFailure(error)
                        }
                    })
            }*/

            if (null != serviceRequest.show && serviceRequest.showId.isNullOrEmpty()) {
                count++

                val show = serviceRequest.show!!.copy()
                putShow(show, object : ResponseCallback<HLServiceShowModel> {
                    override fun onSuccess(data: HLServiceShowModel) {
                        serviceRequest.showId = show.uid
                        serviceRequest.show = show
                        count--

                        if (1 > count) saveServiceRequest()
                    }

                    override fun onFailure(error: String) {
                        callback.onFailure(error)
                    }
                })
            }
        }

        if (0 == count) saveServiceRequest()
    }


    fun putInvoice(invoice: HLInvoiceModel, saveAsDraft: Boolean = false, callback: ResponseCallback<HLInvoiceModel>) {
        // Save service requests.
        if (invoice.requests.isNullOrEmpty()) {
            callback.onFailure("Need one service request at least.")
            return
        }

        fun saveInvoice() {
            if (saveAsDraft) {
                var amount = 0.0
                invoice.requests?.forEach { request ->
                    request.services.forEach { amount += it.rate * it.quantity.toFloat() }
                }
                invoice.amount = amount
                val requests = invoice.requests?.filter { it.services.count() > 0 }
                requests?.let {
                    invoice.requests = ArrayList(it)
                }
                val invoiceIds = invoice.requests?.map { it.uid }
                invoiceIds?.let {
                    invoice.requestIds = ArrayList(it)
                }
                callback.onSuccess(invoice)
                return
            }

            // Set again requestIds with updated ones.
            val invoiceIds = invoice.requests?.map { it.uid }
            invoiceIds?.let {
                invoice.requestIds = ArrayList(it)
            }
            val cloned = invoice.copy()
            var uid = cloned.uid
            if (uid.isEmpty()) {
                uid = collectionInvoices.document().id
                cloned.uid = uid
            }

            collectionInvoices.document(uid).set(cloned, SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        invoice.uid = uid
                        callback.onSuccess(invoice)
                    } else {
                        callback.onFailure(it.exception?.localizedMessage ?: "Something went wrong")
                    }
                }.addOnFailureListener {
                    callback.onFailure(it.localizedMessage ?: "Something went wrong")
                }
        }

        invoice.updatedAt = Date().time

        var count = invoice.requests!!.size

        invoice.requests!!.forEach { request ->
            if (request.services.size == 0) {
                deleteServiceRequest(request.uid, object : ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        count--
                        if (1 > count) saveInvoice()
                    }

                    override fun onFailure(error: String) {
                        callback.onFailure(error)
                    }
                })
            } else {
                val cloned = request.copy()
                putServiceRequest(cloned, object : ResponseCallback<HLServiceRequestModel> {
                    override fun onSuccess(data: HLServiceRequestModel) {
                        request.uid = data.uid
                        count--
                        if (1 > count) saveInvoice()
                    }

                    override fun onFailure(error: String) {
                        callback.onFailure(error)
                    }
                })
            }

        }
    }

    fun updateInvoice (invoiceId: String, updateData: HashMap<String, Any>, callback: ResponseCallback<String>) {
        collectionInvoices.document(invoiceId)
            .update(updateData)
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot update invoice (null)")
            }
    }

    
    /**
     * Payments
     */
    fun exportInvoices (userId: String, status: String?, startDate: Long?, endDate: Long?, serviceProviderIds: List<String>? = null,
                        horseManagerIds: List<String>? = null, horseIds: List<String>?, callback: ResponseCallback<String>) {
        val data = hashMapOf<String, Any>("userId" to userId)

        status?.let {
            data["status"] = it
        }


        startDate?.let {
            data["startDate"] = it
        }

        endDate?.let {
            data["endDate"] = it
        }

        serviceProviderIds?.let {
            data["serviceProviderIds"] = it
        }

        horseManagerIds?.let {
            data["horseManagerIds"] = it
        }

        horseIds?.let {
            data["horseIds"] = it
        }

        functions.getHttpsCallable("exportInvoices")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as Map<*, *>
                    if (results["message"] != null) {
                        callback.onSuccess(results["message"] as String)
                    } else {
                        callback.onFailure("There is no activity for that timeframe, try again!")
                    }
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot export invoice (null)")
                }
            }
    }


    /**
     *  Get service requests of a service privider
     *
     *  @param
     *
     *  @horseId: horse profile id.
     *  @serviceProviderId: service provider id
     *  @lastRequestId: last request id for pagination.
     *  @limit: how many requests wants
     *  @filter: filter model
     *
     *  return an array of service requests
     */

    fun searchServiceRequestsForProvider(horseId: String?,
                                         serviceProviderId: String?,
                                         requestStatuses: ArrayList<String>?,
                                         lastRequestId: String?,
                                         limit: Long? = HLConstants.LIMIT_SERVICE_REQUESTS,
                                         filter: HLServiceRequestFilterModel?,
                                         callback: ResponseCallback<List<HLServiceRequestModel>>) {

        val data = HashMap<String, Any>()
        horseId?.let {
            data["horseId"] = it
        }

        serviceProviderId?.let {
            data["serviceProviderId"] = it
        }

        requestStatuses?.let {
            data["statuses"] = it
        }

        lastRequestId?.let {
            data["lastRequestId"] = lastRequestId
        }

        limit?.let {
            data["limit"] = limit
        }

        filter?.let { f ->

            f.startDate?.let {
                data["startDate"] = it
            }

            f.endDate?.let {
                data["endDate"] = it
            }

            f.sort?.let { sort ->

                if (sort.name != null && sort.order != null) {
                    data[sort.name!!] = sort.order!!
                }
            }

        }

        // call function
        functions.getHttpsCallable("searchServiceRequests")
            .call(data).continueWith { task ->
                return@continueWith task.result?.data
            }.addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                      val results = task.result as ArrayList<*>
                      val requests = ArrayList<HLServiceRequestModel> ()

                      for (index in results.indices) {
                          results[index]?.toCustomObject(HLServiceRequestModel::class.java)?.let { request ->
                              requests.add(request)
                          }
                      }
                      callback.onSuccess(requests)
                  } else {
                      callback.onFailure(task.exception?.localizedMessage ?: "Cannot get service requests (null)")
                  }
            }
    }

    /**
     *  Manager Service Provider Handlers
     */
    fun getHorseManagerProviders (userId: String, callback: ResponseCallback<List<HLHorseManagerProviderModel>>) {
        collectionManagerServiceProviders.whereEqualTo("creatorId", userId)
            .orderBy("serviceType")
            .get()
            .addOnSuccessListener { querySnapshots ->
                val providers = ArrayList<HLHorseManagerProviderModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLHorseManagerProviderModel::class.java)?.let {
                        providers.add(it)
                    }
                }
                callback.onSuccess(providers)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get horse manager data!")
            }
    }

    fun addHorseManagerProvider(provider: HLHorseManagerProviderModel, callback: ResponseCallback<String>) {
        val ref = collectionManagerServiceProviders.document()
        provider.uid = ref.id
        ref.set(provider, SetOptions.merge())
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot add manager service provider (null)")
            }
    }

    fun deleteHorseManagerProvider (uid: String, callback: ResponseCallback<String>) {
        collectionManagerServiceProviders.document(uid)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(successMsg)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot delete provider (null)")
            }
    }



    /**
     * Search invoices for both type of users
     *
     * @parameters:
     * @userId: String. service provider user id or horse manager user id
     * @statuses: [HLInvoiceStatusType]?. Invoice statues
     * @limit: Int? = HLSearchLimit.invoices. Used for pagination
     * @lastInvoiceId: String?. Used for pagination
     * @callback: (([HLInvoiceModel]?, String?) -> Void)?
     *
     * @return The array of invoices. [HLInvoiceModel]?
     *
     *
     */
    fun getInvoices(userId: String,
                    statuses: ArrayList<String>? = arrayListOf(),
                    limit: Long? = HLConstants.LIMIT_INVOICES,
                    lastInvoiceId: String?,
                    callback: ResponseCallback<List<HLInvoiceModel>>) {

        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "limit" to (limit?: HLConstants.LIMIT_INVOICES)
        )

        statuses?.let {
            data["statuses"] = it
        }

        lastInvoiceId?.let {
            data["lastInvoiceId"] = it
        }

        functions.getHttpsCallable("searchInvoices")
            .call(data).continueWith { task ->
            return@continueWith task.result?.data
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val results = task.result as ArrayList<*>
                val invoices = ArrayList<HLInvoiceModel> ()

                for (index in results.indices) {
                    results[index]?.toCustomObject(HLInvoiceModel::class.java)?.let { request ->
                        invoices.add(request)
                    }
                }
                callback.onSuccess(invoices)
            } else {
                callback.onFailure(task.exception?.localizedMessage ?: "Cannot get service requests (null)")
            }
        }
    }


    /**
     * Subscribe invoice draft listener
     */
    fun subscribeServiceRequestsCompletedListener() {

        listenerServiceRequestsCompleted?:let {

            val userId: String?
            val userType: String
            if (HLGlobalData.me?.type == HLUserType.PROVIDER) {
                userId = HLGlobalData.me?.serviceProvider?.userId
                userType = HLUserType.PROVIDER
            } else {
                userId = HLGlobalData.me?.horseManager?.userId
                userType = HLUserType.MANAGER
            }

            userId?.let {
                val params = hashMapOf(
                    "userId" to userId,
                    "userType" to userType
                )

                listenerServiceRequestsCompleted = collectionServiceRequests
                    .whereArrayContains("listenerUsers", params)
                    .whereEqualTo("status", HLServiceRequestStatus.COMPLETED)
                    .whereGreaterThanOrEqualTo("updatedAt", Calendar.getInstance().timeInMillis)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, exception ->
                        if (exception != null) {
                            print(exception.localizedMessage)
                        }

                        val changes = snapshot?.documentChanges?.map { it.document }
                        val statuses = snapshot?.documentChanges?.map { it.type }

                        if (changes != null && statuses != null) {
                            val requestIdes = arrayListOf<String>()
                            changes.forEach { change ->
                                val request = change.toObject(HLServiceRequestModel::class.java)
                                requestIdes.add(request.uid)
                            }

                            EventBus.getDefault().post(HLEventServiceRequestsCompleted(requestIdes, statuses))

                        }
                    }
            }
        }

    }

    /**
     * Subscribe the observer to update the states of invoices (submitted and paid)
     */
    fun subscribePaymentStatusUpdatedListener() {

        listenerPaymentStatusUpdated?:let {

            val userId: String?
            val userType: String
            if (HLGlobalData.me?.type == HLUserType.PROVIDER) {
                userId = HLGlobalData.me?.serviceProvider?.userId
                userType = HLUserType.PROVIDER
            } else {
                userId = HLGlobalData.me?.horseManager?.userId
                userType = HLUserType.MANAGER
            }

            userId?.let {
                val params = hashMapOf(
                    "userId" to userId,
                    "userType" to userType
                )

                listenerPaymentStatusUpdated = collectionInvoices
                    .whereArrayContains("listenerUsers", params)
                    .whereGreaterThanOrEqualTo("updatedAt", Calendar.getInstance().timeInMillis)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, _ ->
                        val changes = snapshot?.documentChanges?.map { it.document }
                        val statuses = snapshot?.documentChanges?.map { it.type }

                        if (changes != null && statuses != null) {
                            val invoiceIds = arrayListOf<String>()
                            changes.forEach { change ->
                                val invoice = change.toObject(HLInvoiceModel::class.java)
                                invoiceIds.add(invoice.uid)
                            }
                            EventBus.getDefault().post(HLEventPaymentInvoicesUpdated(invoiceIds, statuses))
                        }
                    }
            }
        }
    }

    /**
     * Subscribe an observer to detect the update of an invoice.
     */

    fun subscribeInvoiceStatusUpdatedListener(invoiceId: String) {

        listenerInvoiceUpdate?:let {
            collectionInvoices.document(invoiceId)
                .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, exception ->
                    if (exception != null) {
                        print(exception.localizedMessage)
                    }

                    snapshot?.data?.let {
                        try {
                            val invoice = it.toCustomObject(HLInvoiceModel::class.java)
                            invoice?.uid?.let { invoiceId ->
                                EventBus.getDefault().post(HLEventPaymentInvoiceUpdated(invoiceId))
                            }
                        } catch (e: Exception) {
                            EventBus.getDefault().post(HLEventPaymentInvoiceUpdated(""))
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    /**
     * Remove an observer to detect the update of an invoice.
     */
    fun removeInvoiceStatusUpdatedListener() {
        listenerInvoiceUpdate?.remove()
        listenerInvoiceUpdate = null
    }

    /**
     * Get Invoice Handler
     */
    fun getInvoice (invoiceId: String, callback: ResponseCallback<HLInvoiceModel>) {
        collectionInvoices.document(invoiceId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val invoice = documentSnapshot.toObject(HLInvoiceModel::class.java)
                if (invoice != null) {
                    callback.onSuccess(invoice)
                } else {
                    callback.onFailure("Cannot get invoice detail!")
                }
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get invoice detail (null)")
            }
    }


    /**
     * Get invoices with invoice ides
     * @parameters:
     * @invoiceIds: [String], invoice ides
     * @completion: ResponseCallback<List<HLInvoiceModel>>
     *
     * @return List<HLInvoiceModel>
     */
    fun getInvoices(invoiceIds: List<String>,
                    callback: ResponseCallback<List<HLInvoiceModel>>) {

        val data = hashMapOf<String, Any>(
            "invoiceIds" to invoiceIds
        )

        functions.getHttpsCallable("getInvoices")
            .call(data)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val results = task.result as ArrayList<*>
                val invoices = ArrayList<HLInvoiceModel> ()

                for (index in results.indices) {
                    results[index]?.toCustomObject(HLInvoiceModel::class.java)?.let { request ->
                        invoices.add(request)
                    }
                }
                callback.onSuccess(invoices)
            } else {
                callback.onFailure(task.exception?.localizedMessage ?: "Cannot get invoices")
            }
        }
    }

    /**
     * Mark an invoice as paid.
     *
     * @parameters:
     * @serviceProviderId: String. service provider id to mark as paid
     * @invoiceId: String. invoice id
     *
     * @return true / false
     */

    fun markInvoiceAsPaid(serviceProviderId: String,
                          invoiceId: String,
                          callback: ResponseCallback<Boolean>) {
        val params = hashMapOf<String, Any>(
            "serviceProviderId" to serviceProviderId,
            "invoiceId" to invoiceId
        )

        functions.getHttpsCallable("markInvoiceAsPaid")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot mark an invoice as paid."
                    )
                }
            }
    }

    /**
     * Send payment reminder to service provider
     *
     * @parameters:
     * @assignerId: String. assigner id
     * @serviceProviderId: String. provider user id to be get a notification
     * @requestIds: [String]. request ids to be get paid
     *
     * return true/false
     */

    fun requestPaymentSubmission(assignerId: String,
                                 serviceProviderId: String,
                                 requestIds: ArrayList<String>,
                                 callback: ResponseCallback<Boolean>) {
        val params = hashMapOf<String, Any>(
            "assignerId" to assignerId,
            "serviceProviderId" to serviceProviderId,
            "requestIds" to requestIds
        )

        functions.getHttpsCallable("requestPaymentSubmission")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot send payment reminder"
                    )
                }
            }
    }

    /**
     * Request the payment to owners by service provider or horse trainer
     *
     * @parameters:
     * @invoiceId: String. The invoice id
     *
     * @return true/false
     */
    fun requestPayment(invoiceId: String,
                       callback: ResponseCallback<Boolean>) {

        val params = hashMapOf<String, Any>(
            "invoiceId" to invoiceId
        )

        functions.getHttpsCallable("requestPayment")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot request the payment"
                    )
                }
            }
    }

    /**
     * Request a payment approval.
     * A payment approver requests payment to invoice payer.
     *
     * @parameters:
     * @approverId: String. approver id
     * @ownerId: String. payer id
     * @amount: Double. amount
     *
     * @return true/false
     */

    fun requestPaymentApproval(approverId: String,
                               payerId: String,
                               amount: Double,
                               callback: ResponseCallback<Boolean>) {

        val params = hashMapOf(
            "userId" to approverId,
            "ownerId" to payerId,
            "amount" to amount
        )

        functions.getHttpsCallable("requestPaymentApproval")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot request the payment"
                    )
                }
            }
    }


    /**
     * Submit the invoice payment by horse manager and payment approver
     *
     * @parameters:
     * @invoiceId: String.
     * @payerId: String.
     * @paymentApproverId: String? needed if approver is procceeded the payment
     * @payerPaymentSourceId: String? needed if approver is procceeded the payment
     * @googleSource: String? needed if payment has been done by apple card
     *
     * @return true/false
     */

    fun submitInvoicePayment(invoiceId: String,
                             payerId: String,
                             paymentApproverId: String?,
                             payerPaymentSourceId: String?,
                             googleSource: String?,
                             callback: ResponseCallback<Boolean>) {

        val params = hashMapOf<String, Any>(
            "invoiceId" to invoiceId,
            "payerId" to payerId
        )

        paymentApproverId?.let {
            params["paymentApproverId"] = it
        }

        payerPaymentSourceId?.let {
            params["sourceId"] = it
        }

        googleSource?.let {
            params["googleSource"] = it
        }

        functions.getHttpsCallable("submitInvoicePayment")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onFailure(
                        task.exception?.localizedMessage ?: "Cannot submit the invoice payment"
                    )
                }
            }
    }

    /**
     * Get payments of an invoice from database.
     *
     * @parameters:
     * @invoiceId: String. The invoice id which involves payment history.
     * @completion: (([HLPaymentModel]?, String?) -> Void)?
     * @payment history: [HLPaymentModel]?
     *
     * @return [HLPaymentModel]?
     */
    fun getPaymentsFor(invoiceId: String,
                       callback: ResponseCallback<ArrayList<HLPaymentModel>>) {

        collectionPayments
            .whereEqualTo("invoiceId", invoiceId)
            .get()
            .addOnSuccessListener { querySnapshots ->
                val payments = ArrayList<HLPaymentModel> ()
                for (doc in querySnapshots.documents) {
                    doc.toObject(HLPaymentModel::class.java)?.let {
                        payments.add(it)
                    }
                }
                callback.onSuccess(payments)
            }
            .addOnFailureListener { ex ->
                callback.onFailure(ex.localizedMessage ?: "Cannot get approvers (null)")
            }
    }

    /**
     * Delete an invoice
     */
    fun deleteInvoice (invoiceId: String, callback: ResponseCallback<Boolean>) {
        collectionInvoices.document(invoiceId)
            .delete()
            .addOnSuccessListener {
                callback.onSuccess(true)
            }
            .addOnFailureListener {
                callback.onFailure(it.localizedMessage ?: "Cannot delete the invoice")
            }
    }


    /**
     * Delete service requests for a draft
     */

    fun deleteDraft(requests: ArrayList<HLServiceRequestModel>, callback: ResponseCallback<Boolean>) {

        var count = 0
        var isFailed = false
        for (request in requests) {

            if (isFailed) {
                callback.onFailure("Failed to delete a draft")
                return
            }

            deleteServiceRequest(request.uid, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    count ++
                    if (count == requests.size && !isFailed) {
                        callback.onSuccess(true)
                    }
                }

                override fun onFailure(error: String) {
                    isFailed = true
                    callback.onFailure(error)
                    count ++
                }
            })
        }
    }

    /**
     * Phone Contact User Handlers
     */
    fun searchContactUsers (phoneNumbers: ArrayList<String>, emails: ArrayList<String>, lastUserId: String?, callback: ResponseCallback<List<HLProviderHorseModel>>) {
        val params = hashMapOf<String, Any>(
            "phones" to phoneNumbers,
            "emails" to emails
        )

        lastUserId?.let {
            params["lastUserId"] = it
        }

        functions.getHttpsCallable("searchContactUsers")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val results = task.result as ArrayList<*>
                    val horses = ArrayList<HLProviderHorseModel> ()
                    for (index in results.indices) {
                        results[index]?.toCustomObject(HLProviderHorseModel::class.java)?.let { horse ->
                            horses.add(horse)
                        }
                    }
                    callback.onSuccess(horses)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get horses for provider (null)")
                }
            }
    }

    /**
     * Payout Handlers
     */
    fun retrieveAccountInfo (accountId: String, callback: ResponseCallback<HLPaymentAccountModel>) {
        val params = hashMapOf<String, Any>(
            "accountId" to accountId
        )

        functions.getHttpsCallable("retrieveAccountInfo")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.toCustomObject(HLPaymentAccountModel::class.java)?.let { account ->
                        callback.onSuccess(account)
                    } ?: callback.onFailure("Cannot get retrieve account info")
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get retrieve account info (null)")
                }
            }
    }

    fun instantPayout (accountId: String, amount: Float, callback: ResponseCallback<String>) {
        val params = hashMapOf(
            "accountId" to accountId,
            "amount" to amount
        )

        functions.getHttpsCallable("instantPayout")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(successMsg)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot get retrieve account info (null)")
                }
            }
    }

    /**
     *  Share Invoice Handler
     */
    fun shareInvoice (userId: String, horseId: String, invoiceId: String, phone: String? = null, email: String? = null, callback: ResponseCallback<String>) {
        val params = hashMapOf(
            "userId" to userId,
            "userPlatform" to HLPlatformType.ANDROID,
            "horseId" to horseId,
            "invoiceId" to invoiceId
        )

        phone?.let {
            params["phone"] = it
        }

        email?.let {
            params["email"] = it
        }

        functions.getHttpsCallable("shareInvoice")
            .call(params)
            .continueWith { task ->
                return@continueWith task.result?.data
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(successMsg)
                } else {
                    callback.onFailure(task.exception?.localizedMessage ?: "Cannot share invoice (null)")
                }
            }
    }
}