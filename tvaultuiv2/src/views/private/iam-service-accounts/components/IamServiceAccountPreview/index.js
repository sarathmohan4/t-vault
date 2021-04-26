/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable no-nested-ternary */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import SnackbarComponent from '../../../../../components/Snackbar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ViewIamSvcAccountDetails from './components/ViewIamSvcAccount';
import SuccessAndErrorModal from '../../../../../components/SuccessAndErrorModal';

const { small } = mediaBreakpoints;

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
`;

const LoaderWrap = styled.div`
  padding: 10rem 20rem;
  background-color: #2a2e3e;
  outline: none;
`;

const StyledModal = styled(Modal)`
  @-moz-document url-prefix() {
    .MuiBackdrop-root {
      position: absolute;
      height: 105rem;
    }
  }
`;

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const ViewIamServiceAccount = (props) => {
  const {
    refresh,
    iamServiceAccountDetails,
    setViewDetails,
    viewAccountData,
  } = props;
  const classes = useStyles();
  const [open] = useState(true);
  const [status, setStatus] = useState(null);
  const [actionPerformed] = useState(false);
  const [successErrorDetails, setSuccessErrorDetails] = useState({
    title: '',
    desc: '',
  });
  const [successErrorModal, setSuccessErrorModal] = useState(false);
  const [openModal] = useState({
    status: '',
    message: '',
    description: '',
  });

  const isMobileScreen = useMediaQuery(small);

  // toast close handler
  const onToastClose = () => {
    setStatus({});
  };

  const handleCloseModal = async (res) => {
    setViewDetails(false);
    if (res) {
      await refresh();
    }
  };

  const handleSuccessAndDeleteModalClose = () => {
    setSuccessErrorDetails({ title: '', desc: '' });
    setSuccessErrorModal(false);
    handleCloseModal(actionPerformed);
  };

  return (
    <ComponentError>
      <>
        {successErrorModal && (
          <SuccessAndErrorModal
            title={successErrorDetails.title}
            description={successErrorDetails.desc}
            handleSuccessAndDeleteModalClose={() =>
              handleSuccessAndDeleteModalClose()
            }
          />
        )}
        <div>
          {!(openModal?.status === 'open') && !successErrorModal ? (
            <StyledModal
              aria-labelledby="transition-modal-title"
              aria-describedby="transition-modal-description"
              className={classes.modal}
              onClose={() => handleCloseModal(actionPerformed)}
              open={open}
              closeAfterTransition
              BackdropComponent={Backdrop}
              BackdropProps={{
                timeout: 500,
              }}
            >
              <Fade in={open}>
                {!iamServiceAccountDetails || status?.status === 'loading' ? (
                  <LoaderWrap>
                    <LoaderSpinner customStyle={loaderStyle} />
                  </LoaderWrap>
                ) : (
                  <ViewIamSvcAccountDetails
                    iamSvcAccountData={iamServiceAccountDetails}
                    isMobileScreen={isMobileScreen}
                    handleCloseModal={() => handleCloseModal(actionPerformed)}
                    viewAccountData={viewAccountData}
                  />
                )}
              </Fade>
            </StyledModal>
          ) : status?.status === 'loading' ? (
            <ConfirmationModal
              open
              handleClose={() => {}}
              title=""
              description=""
              confirmButton={<LoaderSpinner customStyle={loaderStyle} />}
            />
          ) : (
            <></>
          )}
        </div>
        {status?.status === 'failed' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={status?.message || 'Something went wrong!'}
          />
        )}
        {status?.status === 'success' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={status?.message || 'Request Successful!'}
          />
        )}
      </>
    </ComponentError>
  );
};

ViewIamServiceAccount.propTypes = {
  refresh: PropTypes.func.isRequired,
  setViewDetails: PropTypes.func.isRequired,
  iamServiceAccountDetails: PropTypes.objectOf(PropTypes.any),
  viewAccountData: PropTypes.objectOf(PropTypes.any).isRequired,
};

ViewIamServiceAccount.defaultProps = {
  iamServiceAccountDetails: {},
};

export default ViewIamServiceAccount;
