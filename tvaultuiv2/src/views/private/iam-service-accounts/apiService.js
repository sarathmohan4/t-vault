import api from '../../../services';

// API call to get all service accoounts
const getIamServiceAccountList = () => api.get('/iamserviceaccounts/list');
const getIamServiceAccounts = () => api.get('/iamserviceaccounts');

const fetchIamServiceAccountDetails = (svcName) =>
  api.get(`/iamserviceaccounts/${encodeURIComponent(svcName)}`);

const activateIamServiceAccount = (svcName, iamAccountId) =>
  api.post(
    `/iamserviceaccount/activate?serviceAccountName=${encodeURIComponent(
      svcName
    )}&awsAccountId=${iamAccountId}`,
    {}
  );
// Service account secret API call.
const rotateIamServiceAccountPassword = (payload) =>
  api.post(`/iamserviceaccount/rotate`, payload);

// get password details

const getIamSvcAccountSecrets = (svcName) =>
  api.get(
    `/iamserviceaccounts/folders/secrets?path=iamsvcacc/${encodeURIComponent(
      svcName
    )}`
  );
const getIamServiceAccountPassword = (svcName, secretName) =>
  api.get(
    `/iamserviceaccounts/secrets/${encodeURIComponent(svcName)}/${secretName}`
  );

// API call for users permission
const addUserPermission = (payload) =>
  api.post('/iamserviceaccounts/user', payload);

const deleteUserPermission = (payload) =>
  api.delete('/iamserviceaccounts/user', payload);

// Api call for groups permission
const addGroupPermission = (payload) =>
  api.post('/iamserviceaccounts/group', payload);
const deleteGroupPermission = (payload) =>
  api.delete('/iamserviceaccounts/group', payload);

// Api call for aws application permission
const addAwsPermission = (url, payload) => api.post(url, payload);
const addAwsRole = (payload) => api.post('/iamserviceaccounts/role', payload);
const deleteAwsRole = (payload) =>
  api.delete('/iamserviceaccounts/role', payload);

// Api call for app roles permission
const addAppRolePermission = (payload) =>
  api.post('/iamserviceaccounts/approle', payload);
const deleteAppRolePermission = (payload) =>
  api.delete('/iamserviceaccounts/approle', payload);

// get user details
const getUserDetails = (user) =>
  api.get(`/ldap/getusersdetail/${user}
`);

const createIamServiceAccountSecret = (svcName, iamAccountId) =>
  api.post(
    `/iamserviceaccounts/${iamAccountId}/${encodeURIComponent(svcName)}/keys`,{});

const deleteIamServiceAccountAccessKey = (payload) =>
  api.delete('/iamserviceaccount/secrets/keys', payload);

export default {
  getIamServiceAccounts,
  getIamServiceAccountList,
  rotateIamServiceAccountPassword,
  fetchIamServiceAccountDetails,
  addUserPermission,
  deleteUserPermission,
  activateIamServiceAccount,
  addGroupPermission,
  deleteGroupPermission,
  addAwsPermission,
  addAwsRole,
  addAppRolePermission,
  deleteAppRolePermission,
  deleteAwsRole,
  getUserDetails,
  getIamServiceAccountPassword,
  getIamSvcAccountSecrets,
  createIamServiceAccountSecret,
  deleteIamServiceAccountAccessKey,
};
