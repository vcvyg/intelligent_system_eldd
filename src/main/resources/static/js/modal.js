class ModalManager {
    constructor() {
        this.active = null;
        this.createBase();
    }

    createBase() {
        if (this.overlay) return;
        this.overlay = document.createElement('div');
        this.overlay.className = 'app-modal-overlay';
        this.overlay.innerHTML = `
            <div class="app-modal">
                <div class="app-modal__header">
                    <h3 class="app-modal__title"></h3>
                    <button class="app-modal__close" aria-label="关闭">&times;</button>
                </div>
                <div class="app-modal__body"></div>
                <div class="app-modal__footer">
                    <button class="btn-secondary app-modal__cancel">取消</button>
                    <button class="btn-primary app-modal__confirm">确定</button>
                </div>
            </div>
        `;
        document.body.appendChild(this.overlay);
        this.modal = this.overlay.querySelector('.app-modal');
        this.titleEl = this.overlay.querySelector('.app-modal__title');
        this.bodyEl = this.overlay.querySelector('.app-modal__body');
        this.cancelBtn = this.overlay.querySelector('.app-modal__cancel');
        this.confirmBtn = this.overlay.querySelector('.app-modal__confirm');
        const closeBtn = this.overlay.querySelector('.app-modal__close');

        closeBtn.addEventListener('click', () => this.close());
        this.cancelBtn.addEventListener('click', () => {
            if (this.active?.onCancel) {
                this.active.onCancel();
            }
            this.close();
        });
        this.confirmBtn.addEventListener('click', async () => {
            if (this.active?.onConfirm) {
                const result = await this.active.onConfirm();
                if (result === false) {
                    return;
                }
            }
            this.close();
        });
        this.overlay.addEventListener('click', (e) => {
            if (e.target === this.overlay && this.active?.closeOnBackdrop !== false) {
                this.close();
            }
        });
    }

    open(options = {}) {
        this.createBase();
        this.active = options;
        this.titleEl.textContent = options.title || '提示';
        this.bodyEl.innerHTML = '';
        if (options.content instanceof HTMLElement) {
            this.bodyEl.appendChild(options.content);
        } else if (typeof options.content === 'string') {
            this.bodyEl.innerHTML = options.content;
        } else {
            this.bodyEl.textContent = '';
        }
        this.confirmBtn.textContent = options.confirmText || '确定';
        this.cancelBtn.textContent = options.cancelText || '取消';
        if (options.showCancel === false) {
            this.cancelBtn.style.display = 'none';
        } else {
            this.cancelBtn.style.display = '';
        }
        this.overlay.classList.add('open');
        document.body.classList.add('modal-open');
    }

    close() {
        this.overlay?.classList.remove('open');
        document.body.classList.remove('modal-open');
        this.active = null;
    }
}

window.AppModal = new ModalManager();
window.showModal = (options) => window.AppModal.open(options);
window.closeModal = () => window.AppModal.close();

