'use babel';

import WebBLEView from './web-b-l-e-view';
import { CompositeDisposable } from 'atom';

export default {

  webBLEView: null,
  modalPanel: null,
  subscriptions: null,

  activate(state) {
    this.webBLEView = new WebBLEView(state.webBLEViewState);
    this.modalPanel = atom.workspace.addModalPanel({
      item: this.webBLEView.getElement(),
      visible: false
    });

    // Events subscribed to in atom's system can be easily cleaned up with a CompositeDisposable
    this.subscriptions = new CompositeDisposable();

    // Register command that toggles this view
    this.subscriptions.add(atom.commands.add('atom-workspace', {
      'web-b-l-e:toggle': () => this.toggle()
    }));
  },

  deactivate() {
    this.modalPanel.destroy();
    this.subscriptions.dispose();
    this.webBLEView.destroy();
  },

  serialize() {
    return {
      webBLEViewState: this.webBLEView.serialize()
    };
  },

  toggle() {
    console.log('WebBLE was toggled!');
    return (
      this.modalPanel.isVisible() ?
      this.modalPanel.hide() :
      this.modalPanel.show()
    );
  }

};
