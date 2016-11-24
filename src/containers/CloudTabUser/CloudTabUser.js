import { h, Component } from 'preact';
import { connect } from 'preact-redux';
import { Button } from 'topcoat-preact';

import Modal from 'containers/Modal';
import ModalPane from 'components/ModalPane';
import CloudUserPane from 'components/CloudUserPane';
import { pgbAppsRequested, fetchApps } from 'actions/pgbSessionActions';

class CloudTabUser extends Component {
  constructor() {
    super();
    this.state = {
      ...this.state,
      isModalOpen: false,
    };
  }

  componentWillMount() {
    const { dispatch, pgb: { apps, loading, accessToken } } = this.props;
    if (!apps) {
      dispatch(pgbAppsRequested());
    }
    dispatch(fetchApps(accessToken));
  }

  componentWillReceiveProps(nextProps) {
    const { pgb: { apps, loading } } = nextProps;
  }

  handleModalDismiss() {
    this.setState({ isModalOpen: false });
    console.log('modal dismissed');
  }

  handleButtonClick(button, e) {
    this.setState({ isModalOpen: true });
    console.log(`${button} clicked`);
  }

  render(props, state) {
    const { pgb: { loading, apps } } = props;
    return (
      <CloudUserPane
        handleButtonClick={ (button, e) => this.handleButtonClick(button, e) }
        loading={ loading }
        apps={ apps && apps.apps }
      >
        <Modal>
          <ModalPane
            open={ state.isModalOpen }
            onDismiss={ () => this.handleModalDismiss() }
          >
            <p>This is a modal. Close it below.</p>
            <Button clickHandler={ () => this.handleModalDismiss() }>
              <img src="assets/img/S_Close_24_N.svg" alt="close" />
              <span>Cancel</span>
            </Button>
          </ModalPane>
        </Modal>
      </CloudUserPane>
    );
  }
}

function mapStateToProps(state) {
  const { pgb } = state;
  return {
    pgb,
  };
}

export default connect(mapStateToProps)(CloudTabUser);
