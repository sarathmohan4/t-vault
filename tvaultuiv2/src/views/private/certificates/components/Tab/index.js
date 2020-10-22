/* eslint-disable no-else-return */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import { useStateValue } from '../../../../../contexts/globalState';
import apiService from '../../apiService';
import CertificateInformation from '../CertificateInformation';
import CertificatePermission from '../CertificatePermission';
// import { UserContext } from '../../../../../contexts';
// styled components goes here

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
  ${mediaBreakpoints.small} {
    height: 77vh;
  }
`;

const TabContentsWrap = styled('div')`
  height: calc(100% - 6rem);
`;

const TabPanel = (props) => {
  const { children, value, index } = props;

  return (
    <TabPanelWrap
      role="tabpanel"
      hidden={value !== index}
      id={`certs-tabpanel-${index}`}
      aria-labelledby={`cert-tab-${index}`}
    >
      {children}
    </TabPanelWrap>
  );
};

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
};

function a11yProps(index) {
  return {
    id: `safety-tab-${index}`,
    'aria-controls': `safety-tabpanel-${index}`,
  };
}

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    padding: '0 2.1rem',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
    background: 'linear-gradient(to bottom,#151820,#2c3040)',
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '4.8rem',
    boxShadow: 'none',
    borderBottom: '0.3rem solid #222632',
    [theme.breakpoints.down('md')]: {
      height: 'auto',
    },
  },
  tab: {
    minWidth: '9.5rem',
  },
}));

const CertificateSelectionTabs = (props) => {
  const { certificateDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [certificateMetaData, setCertificateMetaData] = useState({});
  const [response, setResponse] = useState({ status: 'loading' });
  const [errorMessage, setErrorMessage] = useState('');
  const [hasPermission, setHasPermission] = useState(false);

  const [state] = useStateValue();
  // const contextObj = useContext(UserContext);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const getAllCertificateDetail = () => {
    setResponse({ status: 'loading' });
    const url = `/sslcert?certificateName=${certificateDetail.certificateName}&certType=${certificateDetail.certType}`;
    apiService
      .getCertificateDetail(url)
      .then((res) => {
        setResponse({ status: 'success' });
        if (res.data.keys && res.data.keys[0]) {
          setCertificateMetaData({ ...res.data.keys[0] });
        } else if (res.data) {
          setCertificateMetaData({ ...res.data });
        } else {
          setCertificateMetaData({});
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMessage(err.response.data.errors[0]);
        }
        setResponse({ status: 'error' });
        setValue(0);
        setHasPermission(false);
      });
  };

  const fetchCertificateDetail = () => {
    setResponse({ status: 'loading' });
    const url = `/sslcert/certificate/${certificateDetail.certType}?certificate_name=${certificateDetail.certificateName}`;
    apiService
      .getCertificateDetail(url)
      .then((res) => {
        setResponse({ status: 'success' });
        if (res.data.keys && res.data.keys[0]) {
          setCertificateMetaData({ ...res.data.keys[0] });
        } else if (res.data) {
          setCertificateMetaData({ ...res.data });
        } else {
          setCertificateMetaData({});
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMessage(err.response.data.errors[0]);
        }
        setResponse({ status: 'error' });
        setValue(0);
        setHasPermission(false);
      });
  };

  /**
   * @function checkCertStatus
   * @description function to check the status of revoked certificate.
   */
  const checkCertStatus = () => {
    setResponse({ status: 'loading' });
    let url = '';
    if (certificateDetail.certificateStatus === 'Revoked') {
      url = `/sslcert/checkstatus/${certificateDetail.certificateName}/${certificateDetail.certType}`;
    } else {
      url = `/sslcert/validate/${certificateDetail.certificateName}/${certificateDetail.certType}`;
    }
    apiService
      .checkCertificateStatus(url)
      .then(() => {
        setResponse({ status: 'success' });
        setCertificateMetaData({ ...certificateDetail });
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          if (
            err.response.data.errors[0] ===
            'Certificate is in Revoke Requested status'
          ) {
            setResponse({ status: 'success' });
            setCertificateMetaData({ ...certificateDetail });
          } else {
            setErrorMessage(err.response.data.errors[0]);
            setResponse({ status: 'error' });
            setHasPermission(false);
            setValue(0);
          }
        }
      });
  };

  useEffect(() => {
    if (Object.keys(certificateDetail).length > 0) {
      if (!certificateDetail?.applicationName) {
        fetchCertificateDetail();
      } else if (
        certificateDetail.certificateStatus === 'Revoked' ||
        !certificateDetail.certificateStatus
      ) {
        checkCertStatus();
      } else {
        setResponse({ status: 'success' });
        setCertificateMetaData({ ...certificateDetail });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [certificateDetail]);

  useEffect(() => {
    if (
      certificateMetaData?.users &&
      Object.keys(certificateMetaData.users).length > 0
    ) {
      const available = Object.keys(certificateMetaData.users).find(
        (key) =>
          certificateMetaData.users[key] === 'write' &&
          key.toLowerCase() === state.username.toLowerCase()
      );
      if (available) {
        setHasPermission(true);
      } else {
        setValue(0);
        setHasPermission(false);
      }
    } else {
      setHasPermission(false);
      setValue(0);
    }
  }, [certificateMetaData, state]);

  return (
    <ComponentError>
      <div className={classes.root}>
        <AppBar position="static" className={classes.appBar}>
          <Tabs
            value={value}
            onChange={handleChange}
            aria-label="cert tabs"
            indicatorColor="secondary"
            textColor="primary"
          >
            <Tab className={classes.tab} label="Info" {...a11yProps(0)} />
            {hasPermission && <Tab label="Permissions" {...a11yProps(1)} />}
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            <CertificateInformation
              responseStatus={response.status}
              certificateMetaData={certificateMetaData}
              errorMessage={errorMessage}
            />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <CertificatePermission
              responseStatus={response.status}
              certificateMetaData={certificateMetaData}
              fetchDetail={getAllCertificateDetail}
              username={state.username}
            />
          </TabPanel>
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
};

CertificateSelectionTabs.propTypes = {
  certificateDetail: PropTypes.objectOf(PropTypes.any),
};
CertificateSelectionTabs.defaultProps = {
  certificateDetail: {},
};

export default CertificateSelectionTabs;
