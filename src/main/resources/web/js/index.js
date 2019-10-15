const { h, Component, createRef, render } = preact;
const { range, interval, defer, fromEvent, Subject, merge } = rxjs;
const { map, filter, catchError, of, switchMap, tap, share, pluck, withLatestFrom, retry } = rxjs.operators;
const { webSocket } = rxjs.webSocket;
const { ajax } = rxjs.ajax;

const html = htm.bind(h);

const vegaOpts = {
    actions: false
};

class App extends Component {
    render() {
        return html`
            <div>
                <div class="hero">
                    <div class="hero-body">
                        <div class="container">
                        <h1 class="title">
                            Nothing here yet
                        </h1>
                        <h2 class="subtitle">
                        </h2>
                        </div>
                    </div>
                </div>
                <div class="container">
                    TODO
                </div>
            </div>
        `;
    }
}

const app = html`<${App} />`
render(app, document.getElementById('app'));